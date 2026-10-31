package com.csf.vcpp.ykc.cmd.v17.up;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.utils.BinUtil;
import com.csf.vcpp.utils.CP56Time2aUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15TradeDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcBillingModelCodec;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V17;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V18;

/**
 * 0x3D 交易记录（V1.7 起由 0x3B 改为 0x3D）。
 * <p>
 * 相对 V1.5 的 0x3B：新增「电表表号 6B + 电表密文 34B + 电表协议版本号 2B + 加密方式 1B」。
 * 深谷（第 5 段）扩展按消息体剩余长度自动识别。
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.TRADE_UP2, protocols = {V17, V18})
public class YkcV17TradeUpCmd extends YkcCmdExecutor {

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

		// ---------- V1.7 新增：电表信息 ----------
		byte[] meterNoBytes = new byte[6];
		body.readBytes(meterNoBytes);
		String meterNo = BcdUtil.toString(meterNoBytes); // 电表表号

		byte[] meterCipherBytes = new byte[34];
		body.readBytes(meterCipherBytes);
		String meterCipher = HexUtil.encodeHexStr(meterCipherBytes).toUpperCase(); // 电表密文

		int meterProtocolVersion = body.readUnsignedShortLE(); // 电表协议版本号
		byte encryptType = body.readByte(); // 加密方式

		// ---------- 尖峰平谷 ----------
		BigDecimal jian = unitPrice(body.readUnsignedIntLE());
		BigDecimal jianKwh = quantity(body.readUnsignedIntLE());
		BigDecimal jianSunKwh = quantity(body.readUnsignedIntLE());
		BigDecimal jianAmount = quantity(body.readUnsignedIntLE());

		BigDecimal feng = unitPrice(body.readUnsignedIntLE());
		BigDecimal fengKwh = quantity(body.readUnsignedIntLE());
		BigDecimal fengSunKwh = quantity(body.readUnsignedIntLE());
		BigDecimal fengAmount = quantity(body.readUnsignedIntLE());

		BigDecimal ping = unitPrice(body.readUnsignedIntLE());
		BigDecimal pingKwh = quantity(body.readUnsignedIntLE());
		BigDecimal pingSunKwh = quantity(body.readUnsignedIntLE());
		BigDecimal pingAmount = quantity(body.readUnsignedIntLE());

		BigDecimal gu = unitPrice(body.readUnsignedIntLE());
		BigDecimal guKwh = quantity(body.readUnsignedIntLE());
		BigDecimal guSunKwh = quantity(body.readUnsignedIntLE());
		BigDecimal guAmount = quantity(body.readUnsignedIntLE());

		// ---------- 深谷（可选） ----------
		BigDecimal deepValley = null;
		BigDecimal deepValleyKwh = null;
		BigDecimal deepValleySunKwh = null;
		BigDecimal deepValleyAmount = null;
		int remaining = body.readableBytes();
		int tailLength = 5 + 5 + 4 + 4 + 4 + 17 + 1 + 7 + 1 + 8;
		if (remaining >= tailLength + 16) {
			deepValley = unitPrice(body.readUnsignedIntLE());
			deepValleyKwh = quantity(body.readUnsignedIntLE());
			deepValleySunKwh = quantity(body.readUnsignedIntLE());
			deepValleyAmount = quantity(body.readUnsignedIntLE());
		}

		byte[] meterStartBytes = new byte[5];
		body.readBytes(meterStartBytes);
		BigDecimal meterStart = new BigDecimal(BinUtil.bin2Long(meterStartBytes)).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

		byte[] meterEndBytes = new byte[5];
		body.readBytes(meterEndBytes);
		BigDecimal meterEnd = new BigDecimal(BinUtil.bin2Long(meterEndBytes)).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);

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
				尖单价: {} 尖电量: {} 计损尖电量: {} 尖金额: {}
				峰单价: {} 峰电量: {} 计损峰电量: {} 峰金额: {}
				平单价: {} 平电量: {} 计损平电量: {} 平金额: {}
				谷单价: {} 谷电量: {} 计损谷电量: {} 谷金额: {}
				深谷单价: {} 深谷电量: {} 计损深谷电量: {} 深谷金额: {}
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
				---------------------------------
				""",
			versionLabel, tradeNo, deviceNo, gunNo, startTime, endTime, meterNo, meterCipher, meterProtocolVersion, encryptType,
			jian, jianKwh, jianSunKwh, jianAmount, feng, fengKwh, fengSunKwh, fengAmount,
			ping, pingKwh, pingSunKwh, pingAmount, gu, guKwh, guSunKwh, guAmount,
			deepValley, deepValleyKwh, deepValleySunKwh, deepValleyAmount,
			meterStart, meterEnd, totalKwh, totalSunKwh, totalAmount, vin, tradeType, tradeTime,
			Integer.toHexString(stopReason & 0xFF).toUpperCase(), cardNo);

		data.setTradeNo(tradeNo);
		data.setDeviceNoBytes(deviceNoBytes);
		ykcV15TradeDownCmd.execute(channel, versionLabel, data);
	}

	private BigDecimal unitPrice(long wire) {
		return YkcBillingModelCodec.toPrice(wire);
	}

	private BigDecimal quantity(long wire) {
		return new BigDecimal(wire).multiply(new BigDecimal("0.0001")).setScale(4, RoundingMode.DOWN);
	}
}
