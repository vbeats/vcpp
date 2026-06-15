package com.csf.vcpp.ykc.cmd.v15.up;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
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

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.HANDSHAKE, protocols = {V15, V16, V17})
public class YkcV15HandShakeUpCmd extends YkcCmdExecutor { // 充电握手
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] tradeNoBytes = new byte[16];
		body.readBytes(tradeNoBytes);
		String tradeNo = BcdUtil.toString(tradeNoBytes);  // 交易流水号

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		byte gunNo = body.readByte();// 枪号

		//BMS 通信协议版本号
		byte[] bmsVersionBytes = new byte[3];
		body.readBytes(bmsVersionBytes);
		String bmsVersion = BcdUtil.toString(bmsVersionBytes);  // BMS 通信协议版本号

		/**
		 *电池类型,01H:铅酸电池;02H:氢
		 * 电池;03H:磷酸铁锂电池;04H:锰
		 * 酸锂电池;05H:钴酸锂电池;06H:
		 * 三元材料电池;07H:聚合物锂离子
		 * 电池;08H:钛酸锂电池;FFH:其他;
		 */
		byte batteryType = body.readByte();// BMS 电池类型

		// BMS 整车动力蓄电池系统额定容量 Ah
		BigDecimal totalBatteryCapacity = new BigDecimal(body.readShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// BMS 整车动力蓄电池系统额定总电压
		BigDecimal totalBatteryVol = new BigDecimal(body.readShortLE()).multiply(new BigDecimal("0.1")).setScale(1, RoundingMode.DOWN);

		// BMS 电池生产厂商名称
		byte[] batteryProducerBytes = new byte[4];
		body.readBytes(batteryProducerBytes);
		String batteryProducer = new String(batteryProducerBytes, StandardCharsets.US_ASCII);

		// BMS 电池组序号
		int batteryGroupNo = body.readIntLE();

		// BMS 电池组生产日期年
		int batteryGroupYear = body.readByte() + 1985;

		// BMS 电池组生产日期月
		byte batteryGroupMonth = body.readByte();

		// BMS 电池组生产日期日
		byte batteryGroupDay = body.readByte();

		// BMS 电池组充电次数
		int batteryGroupChargeTimes = body.readUnsignedMediumLE();

		// BMS 电池组产权标识 （<0>：=租赁；<1>：=车自有）
		byte batteryGroupOwnership = body.readByte();

		// 预留位
		body.skipBytes(1);

		// BMS 车辆识别码 vin
		byte[] vinBytes = new byte[17];
		body.readBytes(vinBytes);
		String vin = new String(vinBytes, StandardCharsets.US_ASCII).replaceAll("\\u0000+$", "");

		// BMS 软件版本号
		byte[] bmsSoftVersionBytes = new byte[8];
		body.readBytes(bmsSoftVersionBytes);

		String bmsSoftVersion = HexUtil.encodeHexStr(bmsSoftVersionBytes);

		log.info("【云快充】{} ⬆️ 充电握手 - 交易流水号: {} 桩号: {} 枪号: {} BMS 通信协议版本号: {} BMS 电池类型: {} BMS 整车动力蓄电池系统额定容量: {}Ah BMS 整车动力蓄电池系统额定总电压: {}V BMS 电池生产厂商名称: {} BMS 电池组序号: {} BMS 电池组生产日期年: {} BMS 电池组生产日期月: {} BMS 电池组生产日期日: {} BMS 电池组充电次数: {} BMS 电池组产权标识: {} VIN: {} BMS 软件版本号: {}",
			versionLabel, tradeNo, deviceId, gunNo, bmsVersion, batteryType, totalBatteryCapacity, totalBatteryVol, batteryProducer, batteryGroupNo, batteryGroupYear, batteryGroupMonth, batteryGroupDay, batteryGroupChargeTimes, batteryGroupOwnership, vin, bmsSoftVersion);
	}
}
