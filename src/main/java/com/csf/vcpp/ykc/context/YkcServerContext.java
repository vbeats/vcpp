package com.csf.vcpp.ykc.context;

import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 云快充服务端上下文：执行器路由表 + 桩号与 TCP 连接的对应关系。
 * <p>
 * 设计要点（面向几十万长连接场景）：
 * </p>
 * <ol>
 *     <li><b>单索引</b>：只维护 {@code 桩号 -> Channel} 一份映射；反向信息（桩号、版本、活跃时间）
 *         通过 {@link #SESSION_KEY} 挂在 Channel 上，避免两份大 Map 带来的内存放大与不一致风险。</li>
 *     <li><b>ChannelGroup</b>：所有连接（含未登录）统一登记，Channel 关闭时由 Netty 自动移除，
 *         不会泄漏；停机时可一次性 {@link #closeAll()}。</li>
 *     <li><b>条件移除</b>：解绑时用 {@code remove(deviceNo, channel)} 做 CAS 移除，
 *         保证「新连接顶替旧连接」场景下不会误删新连接的映射。</li>
 *     <li><b>顶替即断开</b>：同一桩号重连时会主动关闭旧连接，避免半死连接堆积。</li>
 * </ol>
 */
@Slf4j
public final class YkcServerContext {

	/**
	 * 会话挂载点：Channel -> {@link YkcSession}
	 */
	public static final AttributeKey<YkcSession> SESSION_KEY = AttributeKey.valueOf("vcpp.ykc.session");

	/**
	 * 预期在线桩数兜底值（{@code vcpp.ykc-expected-devices} 未配置时使用）。
	 * <p>
	 * ConcurrentHashMap 会按 0.75 负载因子把初始容量放大到 2 的幂，因此
	 * 65536 -> 131072 槽 -> 约 0.5 MB 表；而 100 万 -> 2^21 槽 -> 约 8 MB 表。
	 * 后者在只有几百台桩的部署上是纯粹的浪费，故默认取较小值。
	 * </p>
	 */
	private static final int DEFAULT_EXPECTED_DEVICES = 1 << 16;

	/**
	 * key: 协议版本 V15 / V16 ... + "_" + 帧类型数值
	 * value: cmd 执行器实例
	 */
	public static final Map<String, YkcCmdExecutor> EXECUTER_MAP = new ConcurrentHashMap<>(512);

	/**
	 * 桩号 === Channel（仅登录成功的桩）
	 * <p>
	 * 非 final：初始容量可由 {@link #applyExpectedDevices(Integer)} 在启动期调整。
	 * </p>
	 */
	private static volatile Map<String, Channel> DEVICE_CHANNEL_MAP =
			new ConcurrentHashMap<>(DEFAULT_EXPECTED_DEVICES);

	/**
	 * 全部 TCP 连接（含未登录），Channel 关闭后由 Netty 自动移除
	 */
	private static final ChannelGroup CHANNEL_GROUP =
			new DefaultChannelGroup("vcpp-ykc-channels", GlobalEventExecutor.INSTANCE);

	private YkcServerContext() {
	}

	// ------------------------------------------------------------------ 启动配置

	/**
	 * 按预期在线桩数调整映射初始容量。
	 * <p>
	 * 必须在建立任何连接之前调用（由 {@code YkcCodecConfig} 在容器启动期完成）；
	 * 若已有连接则忽略，避免丢映射。
	 * </p>
	 *
	 * @param expected 预期同时在线桩数，null 或 &lt;= 0 时使用兜底值
	 */
	public static void applyExpectedDevices(Integer expected) {
		int value = expected == null || expected <= 0 ? DEFAULT_EXPECTED_DEVICES : expected;
		if (!DEVICE_CHANNEL_MAP.isEmpty()) {
			log.warn("【云快充】 已有在线连接, 忽略 ykc-expected-devices={} 的容量调整", value);
			return;
		}
		if (value == expectedCapacity) {
			return;
		}
		DEVICE_CHANNEL_MAP = new ConcurrentHashMap<>(value);
		expectedCapacity = value;
		log.info("【云快充】 桩号映射初始容量: {} (表约 {} KB)", value, estimateTableBytes(value) / 1024);
	}

	/**
	 * 上一次生效的初始容量，避免重复替换
	 */
	private static int expectedCapacity = DEFAULT_EXPECTED_DEVICES;

	/**
	 * 估算 ConcurrentHashMap 内部 table 数组的字节数：容量按 1.5 倍放大后向上取 2 的幂，
	 * 每个槽 4 字节（压缩指针）。
	 */
	private static long estimateTableBytes(int expected) {
		long target = Math.min((long) expected + (expected >>> 1) + 1, 1L << 30);
		long cap = 1L;
		while (cap < target) {
			cap <<= 1;
		}
		return cap << 2;
	}

	// ------------------------------------------------------------------ 连接生命周期

	/**
	 * 连接建立：只登记到 ChannelGroup，不建立桩号映射
	 */
	public static void attach(Channel channel, String versionLabel) {
		if (channel == null) {
			return;
		}
		CHANNEL_GROUP.add(channel);
		YkcSession session = new YkcSession();
		session.setVersionLabel(versionLabel);
		session.setRemoteAddress(channel.remoteAddress());
		session.setLastActiveTime(java.time.LocalDateTime.now());
		channel.attr(SESSION_KEY).set(session);
	}

	/**
	 * 登录成功：建立 桩号 -> Channel 映射。
	 * 若该桩号已存在其它连接，旧连接会被主动关闭（桩重连 / 网络闪断场景）。
	 *
	 * @return 被顶替掉的旧连接（可能为 null）
	 */
	public static Channel bind(Channel channel, String deviceNo) {
		if (channel == null || deviceNo == null || deviceNo.isBlank()) {
			return null;
		}

		YkcSession session = channel.attr(SESSION_KEY).get();
		if (session == null) {
			session = new YkcSession();
			channel.attr(SESSION_KEY).set(session);
		}
		session.setDeviceNo(deviceNo);
		session.setLoginTime(java.time.LocalDateTime.now());
		session.setAuthenticated(true);
		session.touch();

		Channel previous = DEVICE_CHANNEL_MAP.put(deviceNo, channel);
		if (previous != null && previous != channel && previous.isActive()) {
			log.warn("【云快充】 桩号: {} 重复登录, 关闭旧连接: {}", deviceNo, previous.remoteAddress());
			previous.close();
		}
		return previous == channel ? null : previous;
	}

	/**
	 * 连接断开 / 异常：移除映射关系。
	 * 使用条件移除，避免误删已被新连接覆盖的映射。
	 *
	 * @return 被解绑的桩号（未登录则为 null）
	 */
	public static String unbind(Channel channel) {
		if (channel == null) {
			return null;
		}

		CHANNEL_GROUP.remove(channel);

		YkcSession session = channel.attr(SESSION_KEY).getAndSet(null);
		if (session == null) {
			return null;
		}

		String deviceNo = session.getDeviceNo();
		if (deviceNo != null) {
			DEVICE_CHANNEL_MAP.remove(deviceNo, channel);
		}
		return deviceNo;
	}

	/**
	 * 刷新最近活跃时间（收到任意合法上行帧时调用）
	 */
	public static void touch(Channel channel) {
		if (channel == null) {
			return;
		}
		YkcSession session = channel.attr(SESSION_KEY).get();
		if (session != null) {
			session.touch();
		}
	}

	// ------------------------------------------------------------------ 查询

	/**
	 * 按桩号查连接；只返回仍然活跃的连接
	 */
	public static Channel getChannel(String deviceNo) {
		if (deviceNo == null) {
			return null;
		}
		Channel channel = DEVICE_CHANNEL_MAP.get(deviceNo);
		if (channel != null && !channel.isActive()) {
			// 兜底：极端情况下映射残留，顺手清理
			DEVICE_CHANNEL_MAP.remove(deviceNo, channel);
			return null;
		}
		return channel;
	}

	public static YkcSession getSession(Channel channel) {
		return channel == null ? null : channel.attr(SESSION_KEY).get();
	}

	public static String getDeviceNo(Channel channel) {
		YkcSession session = getSession(channel);
		return session == null ? null : session.getDeviceNo();
	}

	/**
	 * 已登录桩数量
	 */
	public static int onlineCount() {
		return DEVICE_CHANNEL_MAP.size();
	}

	/**
	 * 全部 TCP 连接数量（含未登录）
	 */
	public static int connectionCount() {
		return CHANNEL_GROUP.size();
	}

	/**
	 * 停机 / 运维：立即关闭所有连接并清空映射
	 */
	public static void closeAll() {
		int size = CHANNEL_GROUP.size();
		if (size > 0) {
			log.warn("【云快充】 关闭全部 TCP 连接, 当前连接数: {}", size);
		}
		Collection<Channel> snapshot = new ArrayList<>(CHANNEL_GROUP);
		for (Channel channel : snapshot) {
			unbind(channel);
		}
		CHANNEL_GROUP.close().awaitUninterruptibly();
		DEVICE_CHANNEL_MAP.clear();
	}

	/**
	 * 关闭指定协议版本的全部连接（单端口下线 / 停机）
	 */
	public static void closeByVersion(String versionLabel) {
		if (versionLabel == null) {
			return;
		}
		List<Channel> targets = new ArrayList<>();
		for (Channel channel : CHANNEL_GROUP) {
			YkcSession session = channel.attr(SESSION_KEY).get();
			if (session != null && versionLabel.equals(session.getVersionLabel())) {
				targets.add(channel);
			}
		}
		if (!targets.isEmpty()) {
			log.warn("【云快充】{} 关闭连接数: {}", versionLabel, targets.size());
			targets.forEach(Channel::close);
		}
	}

	/**
	 * 运维：列出当前在线桩号（数量可能极大，仅用于排查）
	 */
	public static List<String> onlineDeviceNos(int limit) {
		List<String> result = new ArrayList<>(Math.min(limit, 1024));
		for (String deviceNo : DEVICE_CHANNEL_MAP.keySet()) {
			result.add(deviceNo);
			if (result.size() >= limit) {
				break;
			}
		}
		return result;
	}
}
