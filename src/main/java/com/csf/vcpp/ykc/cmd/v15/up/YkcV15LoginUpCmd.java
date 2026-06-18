package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.api.ApiClient;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15LoginDownCmd;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15TimeSyncDownCmd;
import com.csf.vcpp.ykc.context.YkcServerContext;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.LOGIN, protocols = {V15, V16, V17})
public class YkcV15LoginUpCmd extends YkcCmdExecutor {    // 充电桩登录上送

	private final YkcV15LoginDownCmd ykcV15LoginDownCmd;
	private final YkcV15TimeSyncDownCmd ykcV15TimeSyncDownCmd;
	private final ApiClient apiClient;

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		short pileType = body.readUnsignedByte(); // 桩类型 0 表示直流桩，1 表示交流桩

		short gunCount = body.readUnsignedByte(); // 枪数量

		short protocolVersion = body.readUnsignedByte(); // 通信协议版本

		byte[] mcuVersionBytes = new byte[8];
		body.readBytes(mcuVersionBytes);
		String mcuVersion = new String(mcuVersionBytes, StandardCharsets.US_ASCII).replaceAll("\\u0000+$", "");

		short netType = body.readUnsignedByte(); // 网络连接类型

		byte[] simBytes = new byte[10];
		body.readBytes(simBytes);
		long sim = BcdUtil.bcdBytesToLong(simBytes); // sim卡号

		/**
		 * 0x00 移动
		 * 0x02 电信
		 * 0x03 联通
		 * 0x04 其他
		 */
		short supplier = body.readUnsignedByte(); // 运营商

		log.info("""
			【云快充】{} ⬆️ 登录请求 - 桩号: {}
			桩类型: {}[{}]
			枪数量: {}
			通信协议版本: {}
			mcu程序版本: {}
			网络连接类型: {}
			sim卡号: {}
			运营商: {}[{}]
			---------------------------------
			""", versionLabel, deviceId, pileType, getPileTypeDesc(pileType), gunCount, protocolVersion, mcuVersion, netType, sim, supplier, getSupplierDesc(supplier));

		// todo login valid
		// R res = apiClient.login();

		boolean loginValid = true;

		// 登录认证应答
		byte loginRes = 0x01; // 0x00：登陆成功 0x01:登陆失败

		if (loginValid) {
			YkcServerContext.DEVICE_CHANNEL_MAP.put(deviceId, channel);
			YkcServerContext.CHANNEL_DEVICE_MAP.put(channel.id().asLongText(), deviceId);
			loginRes = 0x00;
		} else {
			log.warn("【云快充】{} ⬆️ 登录请求 - 桩号: {} 登录验证未通过", versionLabel, deviceId);
		}

		data.setDeviceIdBytes(deviceIdBytes);

		ykcV15LoginDownCmd.execute(channel, versionLabel, data);

		// 下发对时设置
		if (loginRes == 0x00) {
			ykcV15TimeSyncDownCmd.execute(channel, versionLabel, data);
		}
	}

	private String getPileTypeDesc(short pileType) {
		return pileType == 0 ? "直流桩" : "交流桩";
	}

	private String getSupplierDesc(short supplier) {
		return switch (supplier) {
			case 0x00 -> "移动";
			case 0x02 -> "电信";
			case 0x03 -> "联通";
			case 0x04 -> "其他";
			default -> "未知";
		};
	}
}
