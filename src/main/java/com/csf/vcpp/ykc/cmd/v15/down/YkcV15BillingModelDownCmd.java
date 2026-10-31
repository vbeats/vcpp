package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.model.BillingModel;
import com.csf.vcpp.model.BillingSection;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.enums.YkcVersion;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import com.csf.vcpp.ykc.utils.YkcBillingModelCodec;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.BILLING_MODEL_ACK, protocols = {V15, V16, V17, V18, V21})
public class YkcV15BillingModelDownCmd extends YkcCmdExecutor { // 计费模型应答（支持深谷扩展 / V2.1 可变费率数量）
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		List<BillingSection> sections = data.getBillingSections();
		if (CollectionUtils.isEmpty(sections)) {
			log.warn("【云快充】{} ⬇️ 计费模型请求应答 - 未配置费率段, 忽略应答", versionLabel);
			return;
		}

		YkcVersion version = YkcVersion.of(versionLabel);
		List<Integer> rates = data.getBillingModels() == null
				? List.of()
				: data.getBillingModels().stream().map(BillingModel::getSectionIndex).toList();

		ByteBuf msgBody = YkcBillingModelCodec.build(version, data.getDeviceNoBytes(), sections, rates);

		log.info("""
				【云快充】{} ⬇️ 计费模型请求应答 - 桩号: {}
				费率段数: {}
				费率: {}
				48时段电价: {}
				---------------------------------
				""", versionLabel, BcdUtil.bcdBytesToHex(data.getDeviceNoBytes()), sections.size(),
				YkcBillingModelCodec.describe(sections), rates);

		ByteBuf res = YkcAckUtil.createAck(version, data.getSequence(), data.getEncryptFlag(), FrameType.BILLING_MODEL_ACK, msgBody);

		if (log.isDebugEnabled()) {
			log.debug("【云快充】{} ⬇️ 计费模型请求应答Hex: {}", versionLabel, ByteBufUtil.hexDump(res).toUpperCase());
		}

		YkcAckUtil.write(channel, res);
	}
}
