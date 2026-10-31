package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15BillingModelDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.enums.YkcVersion;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcDefaultBillingModelProvider;
import com.csf.vcpp.ykc.model.YkcMsgData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x09 计费模型请求（尖峰平谷 / 深谷模式）
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.BILLING_MODEL, protocols = {V15, V16, V17, V18, V21})
public class YkcV15BillingModelUpCmd extends YkcCmdExecutor { // 计费模型请求

	private final YkcV15BillingModelDownCmd ykcV15BillingModelDownCmd;

	private final YkcDefaultBillingModelProvider billingModelProvider;

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] deviceNoBytes = new byte[7];
		body.readBytes(deviceNoBytes);
		String deviceNo = BcdUtil.bcdBytesToHex(deviceNoBytes);  // 桩号

		// V2.1 计费模型请求报文在桩号后携带计费模型编号(2B)
		int billingModelNo = 0x0100;
		if (body.readableBytes() >= 2 && YkcVersion.of(versionLabel).isVariableBillingSections()) {
			billingModelNo = body.readUnsignedShort();
		}

		log.info("【云快充】{} ⬆️ 计费模型请求 - 桩号: {} 计费模型编号: 0x{}", versionLabel, deviceNo,
				Integer.toHexString(billingModelNo).toUpperCase());

		// todo 获取平台计费模型（apiClient），当前使用默认值
		YkcVersion version = YkcVersion.of(versionLabel);

		data.setDeviceNoBytes(deviceNoBytes);
		data.setBillingSections(billingModelProvider.sections(version));
		data.setBillingModels(billingModelProvider.models(version));

		// 应答 0x0A
		ykcV15BillingModelDownCmd.execute(channel, versionLabel, data);
	}
}
