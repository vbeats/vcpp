package com.csf.vcpp.ykc.context;

import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class YkcServerContext {

	// key: 协议版本 V15 / V16 ... + "_" + 帧类型
	// value: cmd执行器实例
	public static final Map<String, YkcCmdExecutor> EXECUTER_MAP = new ConcurrentHashMap<>(64);

	// ----------------- 充电桩登录成功后 放入 -----------------
	// 桩号 === Channel
	public static final Map<Long, Channel> DEVICE_CHANNEL_MAP = new ConcurrentHashMap<>(32);

	// Channel id === 桩号
	public static final Map<String, Long> CHANNEL_DEVICE_MAP = new ConcurrentHashMap<>(32);
}
