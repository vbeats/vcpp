package com.csf.vcpp.ykc.schema;

import java.math.BigDecimal;

/**
 * 帧字段描述。
 * <p>
 * 云快充报文绝大多数字段可归为有限的几类：BCD 数字串、ASCII 串、小端无符号/有符号整数
 * （带比例系数与偏移量）、CP56Time2a 时间、原始字节、位标志。
 * 用声明式描述替代逐字段硬编码解析，既能覆盖全部帧类型，也避免遗漏/错序。
 * </p>
 */
public final class FrameField {

	public enum Kind {
		/** BCD 数字串（交易流水号、卡号等） */
		BCD_TEXT,
		/** BCD 十六进制串（桩号） */
		BCD_HEX,
		/** ASCII 字符串，尾部补 0x00 */
		ASCII,
		/** 小端无符号整数 */
		UINT,
		/** 小端有符号整数 */
		SINT,
		/** CP56Time2a 时间（7 字节） */
		CP56,
		/** 原始字节，按 HEX 展示 */
		BYTES,
		/** 位标志，展示置位的位号 */
		BITS,
		/** 预留/跳过 */
		SKIP
	}

	private final String name;

	private final Kind kind;

	private final int length;

	private final BigDecimal scale;

	private final BigDecimal offset;

	private final int decimals;

	private final String unit;

	private FrameField(String name, Kind kind, int length, BigDecimal scale, BigDecimal offset, int decimals, String unit) {
		this.name = name;
		this.kind = kind;
		this.length = length;
		this.scale = scale;
		this.offset = offset;
		this.decimals = decimals;
		this.unit = unit;
	}

	// ------------------------------------------------------------------ 构造

	public static FrameField bcdText(String name, int length) {
		return new FrameField(name, Kind.BCD_TEXT, length, null, null, 0, null);
	}

	public static FrameField bcdHex(String name, int length) {
		return new FrameField(name, Kind.BCD_HEX, length, null, null, 0, null);
	}

	public static FrameField ascii(String name, int length) {
		return new FrameField(name, Kind.ASCII, length, null, null, 0, null);
	}

	public static FrameField uint(String name, int length) {
		return new FrameField(name, Kind.UINT, length, null, null, 0, null);
	}

	public static FrameField sint(String name, int length) {
		return new FrameField(name, Kind.SINT, length, null, null, 0, null);
	}

	/**
	 * 带比例系数的小端整数：值 = raw * scale
	 */
	public static FrameField scaled(String name, int length, String scale, int decimals) {
		return scaled(name, length, scale, decimals, null);
	}

	public static FrameField scaled(String name, int length, String scale, int decimals, String unit) {
		return new FrameField(name, Kind.UINT, length, new BigDecimal(scale), BigDecimal.ZERO, decimals, unit);
	}

	/**
	 * 带偏移量与比例系数：值 = (raw + offset) * scale
	 */
	public static FrameField offsetScaled(String name, int length, String scale, String offset, int decimals, String unit) {
		return new FrameField(name, Kind.SINT, length, new BigDecimal(scale), new BigDecimal(offset), decimals, unit);
	}

	public static FrameField cp56(String name) {
		return new FrameField(name, Kind.CP56, 7, null, null, 0, null);
	}

	public static FrameField bytes(String name, int length) {
		return new FrameField(name, Kind.BYTES, length, null, null, 0, null);
	}

	/**
	 * 变长字段：读取消息体剩余全部字节（长度 -1）
	 */
	public static FrameField bytesRest(String name) {
		return new FrameField(name, Kind.BYTES, -1, null, null, 0, null);
	}

	public static FrameField asciiRest(String name) {
		return new FrameField(name, Kind.ASCII, -1, null, null, 0, null);
	}

	public static FrameField bits(String name, int length) {
		return new FrameField(name, Kind.BITS, length, null, null, 0, null);
	}

	public static FrameField skip(int length) {
		return new FrameField("预留", Kind.SKIP, length, null, null, 0, null);
	}

	// ------------------------------------------------------------------ 访问

	public String getName() {
		return name;
	}

	public Kind getKind() {
		return kind;
	}

	public int getLength() {
		return length;
	}

	public BigDecimal getScale() {
		return scale;
	}

	public BigDecimal getOffset() {
		return offset;
	}

	public int getDecimals() {
		return decimals;
	}

	public String getUnit() {
		return unit;
	}
}
