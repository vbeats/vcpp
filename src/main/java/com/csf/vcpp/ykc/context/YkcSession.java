package com.csf.vcpp.ykc.context;

import com.csf.vcpp.ykc.enums.YkcVersion;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.net.SocketAddress;
import java.time.LocalDateTime;

/**
 * 一条 TCP 连接对应的充电桩会话。
 * <p>
 * 会话对象通过 {@code Channel#attr(YkcServerContext#SESSION_KEY)} 挂在 Channel 上，
 * 不额外占用全局 Map，避免几十万连接场景下多份索引带来的内存放大。
 * </p>
 */
@Getter
@Setter
public class YkcSession implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

	/**
	 * 桩号（14 位 BCD）
	 */
	private String deviceNo;

	/**
	 * 协议版本标签
	 */
	private String versionLabel;

	/**
	 * 对端地址
	 */
	private transient SocketAddress remoteAddress;

	/**
	 * 登录时间（未登录为 null）
	 */
	private LocalDateTime loginTime;

	/**
	 * 最近一次收到报文的时间（心跳 / 业务帧均刷新）
	 */
	private volatile LocalDateTime lastActiveTime;

	/**
	 * 桩类型：0 直流 1 交流
	 */
	private Short pileType;

	/**
	 * 枪数量
	 */
	private Short gunCount;

	/**
	 * 桩侧上报的通信协议版本
	 */
	private Short protocolVersion;

	/**
	 * 是否已通过登录认证
	 */
	private boolean authenticated;

	public YkcVersion version() {
		return versionLabel == null ? null : YkcVersion.of(versionLabel);
	}

	public void touch() {
		this.lastActiveTime = LocalDateTime.now();
	}
}
