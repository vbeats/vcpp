package com.csf.vcpp.ykc.schema;

import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.utils.CP56Time2aUtil;
import com.csf.vcpp.ykc.utils.YkcSchemaValue;
import io.netty.buffer.ByteBuf;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 帧字段序列（消息体结构描述）。
 * <p>
 * 严格按协议文档中的字段顺序解析；整数统一按小端（LE）读取。
 * </p>
 */
public final class FrameSchema {

	private final List<FrameField> fields;

	private FrameSchema(List<FrameField> fields) {
		this.fields = fields;
	}

	public static FrameSchema of(FrameField... fields) {
		return new FrameSchema(new ArrayList<>(Arrays.asList(fields)));
	}

	public static FrameSchema of(List<FrameField> fields) {
		return new FrameSchema(new ArrayList<>(fields));
	}

	public List<FrameField> getFields() {
		return fields;
	}

	/**
	 * 按顺序解析消息体。消息体不足时提前结束，避免抛异常影响整帧处理。
	 */
	public List<YkcSchemaValue> parse(ByteBuf body) {
		List<YkcSchemaValue> values = new ArrayList<>(fields.size());
		for (FrameField field : fields) {
			int length = field.getLength() < 0 ? body.readableBytes() : field.getLength();
			if (body.readableBytes() < length) {
				break;
			}
			values.add(readOne(body, length, field));
		}
		return values;
	}

	private YkcSchemaValue readOne(ByteBuf body, int length, FrameField field) {
		switch (field.getKind()) {
			case SKIP -> {
				body.skipBytes(length);
				return null;
			}
			case BCD_TEXT -> {
				byte[] raw = readBytes(body, length);
				return new YkcSchemaValue(field.getName(), BcdUtil.toString(raw));
			}
			case BCD_HEX -> {
				byte[] raw = readBytes(body, length);
				return new YkcSchemaValue(field.getName(), BcdUtil.bcdBytesToHex(raw));
			}
			case ASCII -> {
				byte[] raw = readBytes(body, length);
				return new YkcSchemaValue(field.getName(), ascii(raw));
			}
			case UINT -> {
				long raw = readUnsigned(body, length);
				return new YkcSchemaValue(field.getName(), apply(raw, field));
			}
			case SINT -> {
				long raw = readSigned(body, length);
				return new YkcSchemaValue(field.getName(), apply(raw, field));
			}
			case CP56 -> {
				byte[] raw = readBytes(body, 7);
				return new YkcSchemaValue(field.getName(), CP56Time2aUtil.decode(raw));
			}
			case BYTES -> {
				byte[] raw = readBytes(body, length);
				return new YkcSchemaValue(field.getName(), hex(raw));
			}
			case BITS -> {
				long raw = readUnsigned(body, length);
				return new YkcSchemaValue(field.getName(), bits(raw, length * 8));
			}
			default -> {
				body.skipBytes(length);
				return null;
			}
		}
	}

	private Object apply(long raw, FrameField field) {
		BigDecimal value = new BigDecimal(raw);
		if (field.getOffset() != null) {
			value = value.add(field.getOffset());
		}
		if (field.getScale() != null) {
			value = value.multiply(field.getScale());
		}
		if (field.getDecimals() > 0) {
			value = value.setScale(field.getDecimals(), RoundingMode.DOWN);
		}
		return value;
	}

	private static byte[] readBytes(ByteBuf body, int length) {
		byte[] raw = new byte[length];
		body.readBytes(raw);
		return raw;
	}

	/**
	 * 小端无符号整数
	 */
	private static long readUnsigned(ByteBuf body, int length) {
		long value = 0;
		for (int i = 0; i < length; i++) {
			value |= ((long) body.readUnsignedByte()) << (8L * i);
		}
		return value;
	}

	/**
	 * 小端有符号整数（补码）
	 */
	private static long readSigned(ByteBuf body, int length) {
		long value = 0;
		for (int i = 0; i < length; i++) {
			value |= ((long) body.readUnsignedByte()) << (8L * i);
		}
		int bits = length * 8;
		if (bits < 64 && (value & (1L << (bits - 1))) != 0) {
			value -= (1L << bits);
		}
		return value;
	}

	private static String ascii(byte[] raw) {
		String text = new String(raw, StandardCharsets.US_ASCII).replaceAll("\u0000+$", "");
		return text.trim();
	}

	private static String hex(byte[] raw) {
		StringBuilder sb = new StringBuilder(raw.length * 2);
		for (byte b : raw) {
			sb.append(String.format("%02X", b));
		}
		return sb.toString();
	}

	private static String bits(long raw, int width) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < width; i++) {
			if ((raw >>> i & 1L) == 1L) {
				if (sb.length() > 0) {
					sb.append(',');
				}
				sb.append("Bit").append(i + 1);
			}
		}
		return sb.length() == 0 ? "无" : sb.toString();
	}
}
