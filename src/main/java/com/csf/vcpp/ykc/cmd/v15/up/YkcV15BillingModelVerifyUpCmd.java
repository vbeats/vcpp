package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15BillingModelVerifyDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.BILLING_MODEL_VERIFY, protocols = {V15, V16, V17})
public class YkcV15BillingModelVerifyUpCmd extends YkcCmdExecutor { // 计费模型验证请求

	private final YkcV15BillingModelVerifyDownCmd ykcV15BillingVerifyModelDownCmd;

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] deviceNoBytes = new byte[7];
		body.readBytes(deviceNoBytes);
		String deviceNo = BcdUtil.bcdBytesToHex(deviceNoBytes);  // 桩号

		short billingModelNo = body.readShort();  // 计费模型编号  首次连接平台为0

		log.info("【云快充】{} ⬆️ 计费模型验证请求 - 桩号: {}, 计费模型编号: {}", versionLabel, deviceNo, billingModelNo);

		data.setDeviceNoBytes(deviceNoBytes);
		data.setBillingModelNo(billingModelNo);
		ykcV15BillingVerifyModelDownCmd.execute(channel, versionLabel, data);
	}
}
