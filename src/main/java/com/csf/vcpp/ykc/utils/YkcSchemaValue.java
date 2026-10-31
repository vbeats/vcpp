package com.csf.vcpp.ykc.utils;

/**
 * 帧字段解析结果：字段名 + 值。
 */
public final class YkcSchemaValue {

	private final String name;

	private final Object value;

	public YkcSchemaValue(String name, Object value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public Object getValue() {
		return value;
	}

	public String getText() {
		return value == null ? "" : String.valueOf(value);
	}

	@Override
	public String toString() {
		return name + ": " + getText();
	}
}
