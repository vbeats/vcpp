package com.csf.vcpp.ykc.utils;

import com.csf.vcpp.model.BillingSection;
import com.csf.vcpp.ykc.enums.YkcVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 计费模型（0x0A / 0x58）消息体编解码。
 * <p>
 * 费率段数在不同版本下不同：
 * </p>
 * <ul>
 *     <li>V1.5 ~ V1.8：固定 4 段（尖峰平谷），深谷扩展为 5 段（尖峰平谷 + 深谷），按消息体长度识别；</li>
 *     <li>V2.1：段数由「费率数量」字段决定，最多 48 段（自由定价模式）。</li>
 * </ul>
 * <p>
 * 报文布局：桩编号(7) + 计费模型编号(2) + [费率数量(1)，仅V2.1] + 费率段(8×N) + 计损比例(1) + 48时段电价(48)
 * </p>
 */
public final class YkcBillingModelCodec {

	/**
	 * 计费模型编号固定值
	 */
	public static final int BILLING_MODEL_NO = 0x0100;

	/**
	 * 费率段名称：0 尖 1 峰 2 平 3 谷 4 深谷（深谷为 V1.5 扩展方案）
	 */
	private static final String[] SECTION_NAMES = {"尖", "峰", "平", "谷", "深谷"};

	private static final BigDecimal RATE_SCALE = new BigDecimal("100000");

	/**
	 * 单价还原系数：报文值 × 0.00001
	 */
	private static final BigDecimal PRICE_FACTOR = new BigDecimal("0.00001");

	private YkcBillingModelCodec() {
	}

	public static String sectionName(int index) {
		return index >= 0 && index < SECTION_NAMES.length ? SECTION_NAMES[index] : "段" + index;
	}

	/**
	 * 组计费模型消息体
	 *
	 * @param version         协议版本
	 * @param deviceNoBytes   桩号 BCD 7 字节
	 * @param sections        费率段（电费率/服务费率）
	 * @param rateIndexes     48 个时段各自指向的费率段索引
	 */
	public static ByteBuf build(YkcVersion version, byte[] deviceNoBytes, List<BillingSection> sections, List<Integer> rateIndexes) {
		int sectionCount = Math.max(1, sections == null ? 0 : sections.size());
		boolean variable = version.isVariableBillingSections();

		int length = 7 + 2 + (variable ? 1 : 0) + 8 * sectionCount + 1 + 48;
		ByteBuf body = Unpooled.buffer(length, length);

		body.writeBytes(deviceNoBytes);         // 桩编号
		body.writeShort(BILLING_MODEL_NO);      // 计费模型编号 固定 0x0100
		if (variable) {
			body.writeByte(sectionCount);       // 费率数量（仅 V2.1）
		}

		for (BillingSection section : sections) {
			body.writeIntLE(toWire(section.getEp()));
			body.writeIntLE(toWire(section.getSp()));
		}

		body.writeByte(0x00);                   // 计损比例
		for (int i = 0; i < 48; i++) {
			int index = rateIndexes != null && i < rateIndexes.size() ? rateIndexes.get(i) : 0;
			body.writeByte(index & 0xFF);
		}
		return body;
	}

	/**
	 * 解析计费模型消息体（深谷扩展按长度自动识别）
	 */
	public static List<BillingSection> parse(ByteBuf body, YkcVersion version) {
		int readable = body.readableBytes();
		int sectionCount = sectionCount(readable, version, body);
		if (sectionCount <= 0) {
			return List.of();
		}

		body.skipBytes(7);  // 桩编号
		body.skipBytes(2);  // 计费模型编号
		if (version.isVariableBillingSections()) {
			body.skipBytes(1); // 费率数量
		}

		List<BillingSection> sections = new ArrayList<>(sectionCount);
		for (int i = 0; i < sectionCount; i++) {
			BillingSection section = new BillingSection();
			section.setEp(toPrice(body.readUnsignedIntLE()));
			section.setSp(toPrice(body.readUnsignedIntLE()));
			sections.add(section);
		}
		return sections;
	}

	/**
	 * 由消息体长度 / 费率数量字段推算费率段数
	 */
	public static int sectionCount(int bodyLength, YkcVersion version, ByteBuf body) {
		int fixed = version.isVariableBillingSections() ? 59 : 58;
		if (version.isVariableBillingSections() && body != null && body.readableBytes() >= 10) {
			return body.getUnsignedByte(9);
		}
		int count = (bodyLength - fixed) / 8;
		return Math.max(0, Math.min(count, 48));
	}

	/**
	 * 报文值 -> 单价：× 0.00001
	 */
	public static BigDecimal toPrice(long wireValue) {
		return new BigDecimal(wireValue).multiply(PRICE_FACTOR);
	}

	/**
	 * 单价 -> 报文值：× 100000
	 */
	public static int toWire(BigDecimal price) {
		if (price == null) {
			return 0;
		}
		return price.multiply(RATE_SCALE).setScale(0, RoundingMode.DOWN).intValue();
	}

	/**
	 * 费率段描述（用于日志）
	 */
	public static String describe(List<BillingSection> sections) {
		if (sections == null || sections.isEmpty()) {
			return "-";
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < sections.size(); i++) {
			BillingSection section = sections.get(i);
			sb.append(sectionName(i)).append("(电:").append(section.getEp())
					.append(" 服务:").append(section.getSp()).append(") ");
		}
		return sb.toString().trim();
	}
}
