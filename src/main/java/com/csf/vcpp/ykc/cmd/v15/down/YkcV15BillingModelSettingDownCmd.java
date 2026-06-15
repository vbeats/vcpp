package com.csf.vcpp.ykc.cmd.v15.down;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.model.BillingModel;
import com.csf.vcpp.model.BillingSection;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.BILLING_MODEL_SETTING, protocols = {V15, V16, V17, V18})
public class YkcV15BillingModelSettingDownCmd extends YkcCmdExecutor { // 计费模型设置 尖峰平谷模式
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		List<BillingSection> sections = data.getBillingSections();
		BillingSection j = sections.get(0);
		BillingSection f = sections.get(1);
		BillingSection p = sections.get(2);
		BillingSection g = sections.get(3);

		int jianEp = j.getEp().multiply(new BigDecimal(100000)).setScale(0, RoundingMode.DOWN).intValue();
		int jianSp = j.getSp().multiply(new BigDecimal(100000)).setScale(0, RoundingMode.DOWN).intValue();

		int fengEp = f.getEp().multiply(new BigDecimal(100000)).setScale(0, RoundingMode.DOWN).intValue();
		int fengSp = f.getSp().multiply(new BigDecimal(100000)).setScale(0, RoundingMode.DOWN).intValue();

		int pingEp = p.getEp().multiply(new BigDecimal(100000)).setScale(0, RoundingMode.DOWN).intValue();
		int pingSp = p.getSp().multiply(new BigDecimal(100000)).setScale(0, RoundingMode.DOWN).intValue();

		int guEp = g.getEp().multiply(new BigDecimal(100000)).setScale(0, RoundingMode.DOWN).intValue();
		int guSp = g.getSp().multiply(new BigDecimal(100000)).setScale(0, RoundingMode.DOWN).intValue();

		// 48时段电价模型
		List<Integer> rates = data.getBillingModels().stream().map(BillingModel::getSectionIndex).toList();

		ByteBuf msgBody = Unpooled.buffer(90);
		msgBody.writeBytes(data.getDeviceIdBytes()); // 桩号
		msgBody.writeShort(0x0100); // 计费模型编号 固定值 0x0100
		msgBody.writeIntLE(jianEp); // 尖 电费率
		msgBody.writeIntLE(jianSp); // 尖 服务费率

		msgBody.writeIntLE(fengEp); // 峰 电费率
		msgBody.writeIntLE(fengSp); // 峰 服务费率

		msgBody.writeIntLE(pingEp); // 平 电费率
		msgBody.writeIntLE(pingSp); // 平 服务费率

		msgBody.writeIntLE(guEp); //谷 电费率
		msgBody.writeIntLE(guSp); // 谷 服务费率

		msgBody.writeByte(0x00); // 计损比例

		// 48时段电价
		rates.forEach(rate -> msgBody.writeByte(rate.byteValue()));

		log.info("【云快充】{} ⬇️ 计费模型设置 - 桩号: {} 尖电费率: {} 尖服务费率: {} 峰电费率: {} 峰服务费率: {} 平电费率: {} 平服务费率: {} 谷电费率: {} 谷服务费率: {} 48时段电价: {}", versionLabel, BcdUtil.bcdBytesToLong(data.getDeviceIdBytes()), j.getEp(), j.getSp(), f.getEp(), f.getSp(), p.getEp(), p.getSp(), g.getEp(), g.getSp(), rates);

		ByteBuf res = YkcAckUtil.createAck(data.getSequence(), data.getEncryptFlag(), FrameType.BILLING_MODEL_SETTING, msgBody);

		log.info("【云快充】{} ⬇️ 计费模型设置Hex: {}", versionLabel, new String(HexUtil.encodeHex(res.array())).toUpperCase());

		channel.writeAndFlush(res);
	}
}
