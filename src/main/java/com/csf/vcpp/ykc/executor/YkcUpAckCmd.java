package com.csf.vcpp.ykc.executor;

import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

/**
 * 「上行请求 -> 平台确认」型执行器基类。
 * <p>
 * 消息体固定头为「交易流水号(可选) + 桩编号 + 枪号」，解析后调用对应的下行确认帧执行器。
 * </p>
 * <p>
 * 内存约定：上行消息体在此处显式释放，随后放入 {@link YkcMsgData} 的是新构造的确认消息体，
 * 由下行执行器负责释放，保证一进一出、不会泄漏。
 * </p>
 */
@Slf4j
public abstract class YkcUpAckCmd extends YkcCmdExecutor {

	private final FrameType upFrameType;

	private final YkcCmdExecutor ackExecutor;

	private final boolean withTradeNo;

	protected YkcUpAckCmd(FrameType upFrameType, YkcCmdExecutor ackExecutor, boolean withTradeNo) {
		this.upFrameType = upFrameType;
		this.ackExecutor = ackExecutor;
		this.withTradeNo = withTradeNo;
	}

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		String tradeNo = null;
		if (withTradeNo) {
			byte[] tradeNoBytes = new byte[16];
			body.readBytes(tradeNoBytes);
			tradeNo = BcdUtil.toString(tradeNoBytes); // 交易流水号
		}

		byte[] deviceNoBytes = new byte[7];
		body.readBytes(deviceNoBytes);
		byte gunNo = body.readByte(); // 枪号

		// 上行消息体解析完毕，显式释放；后续应答使用新构造的消息体
		YkcAckUtil.release(body);

		data.setTradeNo(tradeNo);
		data.setDeviceNoBytes(deviceNoBytes);
		data.setGunNo(gunNo);

		byte[] resultBytes = resultBytes(data);
		data.setMsgBody(Unpooled.buffer(resultBytes.length).writeBytes(resultBytes));

		log.info("【云快充】{} ⬆️ {} - 交易流水号: {} 桩号: {} 枪号: {}",
				versionLabel, upFrameType.getDesc(), tradeNo, BcdUtil.bcdBytesToHex(deviceNoBytes), gunNo);

		ackExecutor.execute(channel, versionLabel, data);
	}

	/**
	 * 确认消息体的结果字段；默认「成功 + 无原因」
	 */
	protected byte[] resultBytes(YkcMsgData data) {
		return new byte[]{0x00, 0x00};
	}
}
