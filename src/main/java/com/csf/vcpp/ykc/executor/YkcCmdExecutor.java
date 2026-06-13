package com.csf.vcpp.ykc.executor;

import com.csf.vcpp.ykc.model.YkcMsgData;
import io.netty.channel.Channel;

// 云快充 cmd执行器
public abstract class YkcCmdExecutor {

	// 业务执行器
	public abstract void execute(Channel channel, String versionLabel, YkcMsgData data);
}
