package com.csf.vcpp.ykc.executor;

import com.csf.vcpp.cmd.CmdMsg;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.enums.YkcVersion;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import org.springframework.util.StringUtils;

/**
 * 云快充 cmd 执行器基类。
 * <p>
 * 约定：{@link #execute} 内通过 {@code data.getMsgBody()} 读取消息体，
 * 由上行路由在 finally 中统一释放；执行器内部产生的中间 ByteBuf
 * （例如下行消息体）必须通过 {@link #sendAck} 交给组包工具，由其负责释放。
 * </p>
 */
public abstract class YkcCmdExecutor {

	/**
	 * 业务执行器
	 */
	public abstract void execute(Channel channel, String versionLabel, YkcMsgData data);

	/**
	 * 平台下行指令（MQTT）转协议帧数据；默认不支持平台主动下发
	 */
	public YkcMsgData covertCmdData2YkcMsgData(CmdMsg cmdMsg) {
		return null;
	}

	/**
	 * 当前指令是否支持平台主动下发（用于下发前的快速校验，避免 NPE）
	 */
	public boolean supportPlatformCmd() {
		return false;
	}

	protected final YkcVersion versionOf(String versionLabel) {
		return YkcVersion.of(versionLabel);
	}

	/**
	 * 平台下行指令转协议帧数据的公共部分：序列号 / 加密标志 / 桩号
	 */
	protected YkcMsgData platformMsgData(CmdMsg<?> cmdMsg) {
		YkcMsgData data = new YkcMsgData();
		data.setSequence(cmdMsg.getSequence());
		data.setEncryptFlag(cmdMsg.getEncryptFlag());
		String deviceNo = cmdMsg.getDeviceNo();
		data.setDeviceNo(deviceNo);
		if (StringUtils.hasText(deviceNo)) {
			data.setDeviceNoBytes(BcdUtil.numStrToBcdBytes(deviceNo));
		}
		return data;
	}

	/**
	 * 组包并下发应答帧。
	 *
	 * @param msgBody 消息体，所有权在本方法内转移，调用方无需释放
	 */
	protected final void sendAck(Channel channel, String versionLabel, YkcMsgData data, FrameType frameType, ByteBuf msgBody) {
		int sequence = data == null ? 0 : data.getSequence();
		byte encryptFlag = data == null ? 0 : data.getEncryptFlag();
		if (channel == null || !channel.isActive()) {
			YkcAckUtil.release(msgBody);
			return;
		}
		ByteBuf ack = YkcAckUtil.createAck(YkcVersion.of(versionLabel), sequence, encryptFlag, frameType, msgBody);
		YkcAckUtil.write(channel, ack);
	}
}
