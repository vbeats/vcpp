package com.csf.vcpp.ykc.cmd.v15.up;

import com.alibaba.fastjson2.JSON;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.model.BillingModel;
import com.csf.vcpp.model.BillingSection;
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
@ProtocolCmd(value = FrameType.BILLING_MODEL, protocols = {V15, V16, V17})
public class YkcV15BillingModelUpCmd extends YkcCmdExecutor { // 计费模型请求  尖峰平谷模式

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
		data.setBillingSections(JSON.parseArray("[\n" +
			"            {\n" +
			"                \"ep\": \"0.01\",\n" +
			"                \"sp\": \"0.01\"\n" +
			"            },\n" +
			"            {\n" +
			"                \"ep\": \"0.02\",\n" +
			"                \"sp\": \"0.02\"\n" +
			"            },\n" +
			"            {\n" +
			"                \"ep\": \"0.03\",\n" +
			"                \"sp\": \"0.03\"\n" +
			"            },\n" +
			"            {\n" +
			"                \"ep\": \"0.04\",\n" +
			"                \"sp\": \"0.04\"\n" +
			"            }\n" +
			"        ]", BillingSection.class));
		data.setBillingModels(JSON.parseArray("[\n" +
			"    {\n" +
			"        \"start\": \"00:00\",\n" +
			"        \"end\": \"00:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"00:30\",\n" +
			"        \"end\": \"01:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"01:00\",\n" +
			"        \"end\": \"01:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"01:30\",\n" +
			"        \"end\": \"02:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"02:00\",\n" +
			"        \"end\": \"02:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"02:30\",\n" +
			"        \"end\": \"03:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"03:00\",\n" +
			"        \"end\": \"03:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"03:30\",\n" +
			"        \"end\": \"04:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"04:00\",\n" +
			"        \"end\": \"04:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"04:30\",\n" +
			"        \"end\": \"05:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"05:00\",\n" +
			"        \"end\": \"05:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"05:30\",\n" +
			"        \"end\": \"06:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"06:00\",\n" +
			"        \"end\": \"06:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"06:30\",\n" +
			"        \"end\": \"07:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"07:00\",\n" +
			"        \"end\": \"07:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"07:30\",\n" +
			"        \"end\": \"08:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"08:00\",\n" +
			"        \"end\": \"08:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"08:30\",\n" +
			"        \"end\": \"09:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"09:00\",\n" +
			"        \"end\": \"09:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"09:30\",\n" +
			"        \"end\": \"10:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"10:00\",\n" +
			"        \"end\": \"10:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"10:30\",\n" +
			"        \"end\": \"11:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"11:00\",\n" +
			"        \"end\": \"11:30\",\n" +
			"        \"sectionIndex\": 1\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"11:30\",\n" +
			"        \"end\": \"12:00\",\n" +
			"        \"sectionIndex\": 1\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"12:00\",\n" +
			"        \"end\": \"12:30\",\n" +
			"        \"sectionIndex\": 1\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"12:30\",\n" +
			"        \"end\": \"13:00\",\n" +
			"        \"sectionIndex\": 1\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"13:00\",\n" +
			"        \"end\": \"13:30\",\n" +
			"        \"sectionIndex\": 1\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"13:30\",\n" +
			"        \"end\": \"14:00\",\n" +
			"        \"sectionIndex\": 1\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"14:00\",\n" +
			"        \"end\": \"14:30\",\n" +
			"        \"sectionIndex\": 1\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"14:30\",\n" +
			"        \"end\": \"15:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"15:00\",\n" +
			"        \"end\": \"15:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"15:30\",\n" +
			"        \"end\": \"16:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"16:00\",\n" +
			"        \"end\": \"16:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"16:30\",\n" +
			"        \"end\": \"17:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"17:00\",\n" +
			"        \"end\": \"17:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"17:30\",\n" +
			"        \"end\": \"18:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"18:00\",\n" +
			"        \"end\": \"18:30\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"18:30\",\n" +
			"        \"end\": \"19:00\",\n" +
			"        \"sectionIndex\": 0\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"19:00\",\n" +
			"        \"end\": \"19:30\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"19:30\",\n" +
			"        \"end\": \"20:00\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"20:00\",\n" +
			"        \"end\": \"20:30\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"20:30\",\n" +
			"        \"end\": \"21:00\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"21:00\",\n" +
			"        \"end\": \"21:30\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"21:30\",\n" +
			"        \"end\": \"22:00\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"22:00\",\n" +
			"        \"end\": \"22:30\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"22:30\",\n" +
			"        \"end\": \"23:00\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"23:00\",\n" +
			"        \"end\": \"23:30\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    },\n" +
			"    {\n" +
			"        \"start\": \"23:30\",\n" +
			"        \"end\": \"24:00\",\n" +
			"        \"sectionIndex\": 2\n" +
			"    }\n" +
			"]", BillingModel.class));

		// 应答
		ykcV15BillingModelDownCmd.execute(channel, versionLabel, data);
	}
}
