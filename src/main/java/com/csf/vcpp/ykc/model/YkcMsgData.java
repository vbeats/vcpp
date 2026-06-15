package com.csf.vcpp.ykc.model;

import com.csf.vcpp.model.BillingModel;
import com.csf.vcpp.model.BillingSection;
import io.netty.buffer.ByteBuf;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class YkcMsgData implements Serializable {
	@Serial
	private static final long serialVersionUID = 7431393716105578048L;

	// 数据帧头信息

	private int sequence; // 2字节 序列号

	private byte encryptFlag; // 1字节 加密标志 0x00 不加密  0x01 des加密

	private short frameType; // 1字节 帧类型

	private ByteBuf msgBody; // 消息体

	// ---------ack 应答 额外参数
	private byte gunNo; // 枪号

	private byte[] deviceIdBytes; // 7字节 桩号
	private byte loginRes; // 登录结果 0x00 登录成功 0x01 登录失败

	private short billingModelNo; // 计费模型编号

	private List<BillingSection> billingSections; // 计费区间段列表

	private List<BillingModel> billingModels; // 48时段计费模型

	private String tradeNo; // 交易流水号
}
