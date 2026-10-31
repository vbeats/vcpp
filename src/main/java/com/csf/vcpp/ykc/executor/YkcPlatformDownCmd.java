package com.csf.vcpp.ykc.executor;

import com.csf.vcpp.cmd.CmdMsg;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * 平台主动下发的下行帧执行器基类。
 * <p>
 * 消息体固定部分为「交易流水号(可选) + 桩编号 + 枪号(可选)」，
 * 其余业务字段由平台在 {@code CmdMsg#data} 中携带，转换后放入 {@link YkcMsgData#getMsgBody()} 追加下发。
 * 这样各版本/各帧类型只需声明自己的固定头结构，无需为每种组合重复写拼包代码。
 * </p>
 */
@Slf4j
public abstract class YkcPlatformDownCmd extends YkcCmdExecutor {

	private final FrameType frameType;

	private final boolean withTradeNo;

	private final boolean withGunNo;

	protected YkcPlatformDownCmd(FrameType frameType, boolean withTradeNo, boolean withGunNo) {
		this.frameType = frameType;
		this.withTradeNo = withTradeNo;
		this.withGunNo = withGunNo;
	}

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		if (data == null) {
			return;
		}
		ByteBuf extra = data.getMsgBody();
		int extraLength = extra == null ? 0 : extra.readableBytes();

		ByteBuf body = Unpooled.buffer(bodyLength(extraLength));
		try {
			if (withTradeNo) {
				body.writeBytes(tradeNoBytes(data));
			}
			body.writeBytes(deviceNoBytes(data));
			if (withGunNo) {
				body.writeByte(data.getGunNo());
			}
			if (extraLength > 0) {
				body.writeBytes(extra);
			}

			log.info("【云快充】{} ⬇️ {} - 桩号: {}{}", versionLabel, frameType.getDesc(),
					BcdUtil.bcdBytesToHex(data.getDeviceNoBytes()),
					withGunNo ? " 枪号: " + data.getGunNo() : "");
		} finally {
			YkcAckUtil.release(extra);
			data.setMsgBody(null);
		}

		sendAck(channel, versionLabel, data, frameType, body);
	}

	private int bodyLength(int extraLength) {
		return (withTradeNo ? 16 : 0) + 7 + (withGunNo ? 1 : 0) + extraLength;
	}

	private byte[] tradeNoBytes(YkcMsgData data) {
		byte[] bytes = new byte[16];
		if (StringUtils.hasText(data.getTradeNo())) {
			byte[] bcd = BcdUtil.numStrToBcdBytes(data.getTradeNo());
			System.arraycopy(bcd, 0, bytes, 16 - bcd.length, bcd.length);
		}
		return bytes;
	}

	private byte[] deviceNoBytes(YkcMsgData data) {
		if (data.getDeviceNoBytes() != null && data.getDeviceNoBytes().length == 7) {
			return data.getDeviceNoBytes();
		}
		byte[] bytes = new byte[7];
		if (StringUtils.hasText(data.getDeviceNo())) {
			byte[] bcd = BcdUtil.numStrToBcdBytes(data.getDeviceNo());
			System.arraycopy(bcd, 0, bytes, 7 - bcd.length, bcd.length);
		}
		data.setDeviceNoBytes(bytes);
		return bytes;
	}

	@Override
	public YkcMsgData covertCmdData2YkcMsgData(CmdMsg cmdMsg) {
		YkcMsgData data = platformMsgData(cmdMsg);
		applyExtra(cmdMsg, data);
		return data;
	}

	@Override
	public boolean supportPlatformCmd() {
		return true;
	}

	/**
	 * 平台侧附加字段（hex 字符串或字节数组）写入消息体尾部
	 */
	private void applyExtra(CmdMsg<?> cmdMsg, YkcMsgData data) {
		Object payload = cmdMsg.getData();
		if (payload == null) {
			return;
		}
		if (payload instanceof String hex && StringUtils.hasText(hex)) {
			data.setMsgBody(Unpooled.wrappedBuffer(BcdUtil.toBytes(hex)));
			return;
		}
		if (payload instanceof java.util.Map<?, ?> map) {
			Object hexValue = map.get("payloadHex");
			if (hexValue instanceof String hex && StringUtils.hasText(hex)) {
				data.setMsgBody(Unpooled.wrappedBuffer(BcdUtil.toBytes(hex)));
			}
			Object gunNo = map.get("gunNo");
			if (gunNo instanceof Number number) {
				data.setGunNo(number.byteValue());
			}
			Object tradeNo = map.get("tradeNo");
			if (tradeNo instanceof String text) {
				data.setTradeNo(text);
			}
		}
	}
}
