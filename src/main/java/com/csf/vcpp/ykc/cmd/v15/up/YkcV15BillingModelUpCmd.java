package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.api.ApiClient;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15BillingModelDownCmd;
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
@ProtocolCmd(value = FrameType.BILLING_VERIFY_MODEL, protocols = {V15, V16, V17, V18})
public class YkcV15BillingModelUpCmd extends YkcCmdExecutor { // 计费模型请求  尖峰平谷模式

	private final ApiClient apiClient;
	private final YkcV15BillingModelDownCmd ykcV15BillingModelDownCmd;

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		log.info("【云快充】{} ⬆️ 计费模型请求 - 桩号: {}", versionLabel, deviceId);

		// todo 获取 平台计费模型

		//R<> priceRes = apiClient.getBillingModel(params);

		data.setDeviceIdBytes(deviceIdBytes);
		data.setBillingSections(null);
		data.setBillingModels(null);

		// 应答
		ykcV15BillingModelDownCmd.execute(channel, versionLabel, data);
	}
}
