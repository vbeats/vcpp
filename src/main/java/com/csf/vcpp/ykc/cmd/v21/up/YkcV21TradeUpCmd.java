package com.csf.vcpp.ykc.cmd.v21.up;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.utils.BinUtil;
import com.csf.vcpp.utils.CP56Time2aUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15TradeDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x3D 交易记录（V2.1）。
 * <p>
 * V2.1 相对 V1.7 的变化：去掉固定的尖峰平谷四段，改为
 * 「费率时段数量 N + N×(单价/电量/计损电量/金额) + 48×(半小时时段电量)」。
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.TRADE_UP2, protocols = {V21})
public class YkcV21TradeUpCmd extends YkcCmdExecutor {

	private final YkcV15TradeDownCmd ykcV15TradeDownCmd;

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] tradeNoBytes = new byte[16];
		body.readBytes(tradeNoBytes);
		String tradeNo = BcdUtil.toString(tradeNoBytes);  // 交易流水号

		byte[] deviceNoBytes = new byte[7];
		body.readBytes(deviceNoBytes);
		String deviceNo = BcdUtil.bcdBytesToHex(deviceNoBytes);  // 桩号

		byte gunNo = body.readByte();// 枪号

		byte[] startTimeBytes = new byte[7];
		body.readBytes(startTimeBytes);
		LocalDateTime startTime = CP56Time2aUtil.decode(startTimeBytes);

		byte[] endTimeBytes = new byte[7];
		body.readBytes(endTimeBytes);
		LocalDateTime endTime = CP56Time2aUtil.decode(endTimeBytes);

		byte[] meterNoBytes = new byte[6];
		body.readBytes(meterNoBytes);
		String meterNo = BcdUtil.toString(meterNoBytes); // 电表表号

		byte[] meterCipherBytes = new byte[34];
		body.readBytes(meterCipherBytes);
		String meterCipher = HexUtil.encodeHexStr(meterCipherBytes).toUpperCase(); // 电表密文

		int meterProtocolVersion = body.readUnsignedShortLE(); // 电表协议版本号
		byte encryptType = body.readByte(); // 加密方式

		byte[] meterStartBytes = new byte[5];
		body.readBytes(meterStartBytes);
		BigDecimal meterStart = meterValue(meterStartBytes);

		byte[] meterEndBytes = new byte[5];
		body.readBytes(meterEndBytes);
		BigDecimal meterEnd = meterValue(meterEndBytes);

		BigDecimal totalKwh = quantity(body.readUnsignedIntLE());
		BigDecimal totalSunKwh = quantity(body.readUnsignedIntLE());
		BigDecimal totalAmount = quantity(body.readUnsignedIntLE());

		byte[] vinBytes = new byte[17];
		body.readBytes(vinBytes);
		String vin = "";
		if (!"FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF".equalsIgnoreCase(HexUtil.encodeHexStr(vinBytes))) {
			vin = new String(vinBytes, StandardCharsets.US_ASCII).replaceAll("\\u0000+$", "");
		}

		byte tradeType = body.readByte();

		byte[] tradeTimeBytes = new byte[7];
		body.readBytes(tradeTimeBytes);
		LocalDateTime tradeTime = CP56Time2aUtil.decode(tradeTimeBytes);

		byte stopReason = body.readByte();
		long cardNo = body.readLongLE();

		// ---------- 费率时段 ----------
		int rateCount = body.readUnsignedByte();
		List<String> rateDesc = new ArrayList<>(rateCount);
		for (int i = 0; i < rateCount && body.readableBytes() >= 16; i++) {
			BigDecimal price = unitPrice(body.readUnsignedIntLE());
			BigDecimal kwh = quantity(body.readUnsignedIntLE());
			BigDecimal sunKwh = quantity(body.readUnsignedIntLE());
			BigDecimal amount = quantity(body.readUnsignedIntLE());
			rateDesc.add(String.format("第%d段[单价:%s 电量:%s 计损电量:%s 金额:%s]", i + 1, price, kwh, sunKwh, amount));
		}

		// ---------- 48 个半小时时段电量 ----------
		List<BigDecimal> halfHourKwh = new ArrayList<>(48);
		for (int i = 0; i < 48 && body.readableBytes() >= 4; i++) {
			halfHourKwh.add(quantity(body.readUnsignedIntLE()));
		}

		log.info("""
				【云快充】{} ⬆️ 交易记录 - 交易流水号: {}
				桩号: {}
				枪号: {}
				开始时间: {}
				结束时间: {}
				电表表号: {}
				电表密文: {}
				电表协议版本号: {}
				加密方式: {}
				电表总起值: {}
				电表总止值: {}
				总电量: {}
				计损总电量: {}
				消费金额: {}
				vin: {}
				交易标识: {}
				交易时间: {}
				停止原因: 0x{}
				物理卡号: {}
				费率时段数量: {}
				费率明细: {}
				48时段电量: {}
				---------------------------------
				""",
			versionLabel, tradeNo, deviceNo, gunNo, startTime, endTime, meterNo, meterCipher, meterProtocolVersion, encryptType,
			meterStart, meterEnd, totalKwh, totalSunKwh, totalAmount, vin, tradeType, tradeTime,
			Integer.toHexString(stopReason & 0xFF).toUpperCase(), cardNo, rateCount, rateDesc, halfHourKwh);

		data.setTradeNo(tradeNo);
		data.setDeviceNoBytes(deviceNoBytes);
		ykcV15TradeDownCmd.execute(channel, versionLabel, data);
	}

	private BigDecimal meterValue(byte[] bytes) {
		return new BigDecimal(BinUtil.bin2Long(bytes)).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
	}

	private BigDecimal unitPrice(long wire) {
		return new BigDecimal(wire).multiply(new BigDecimal("0.00001")).setScale(5, RoundingMode.DOWN);
	}

	private BigDecimal quantity(long wire) {
		return new BigDecimal(wire).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
	}
}
