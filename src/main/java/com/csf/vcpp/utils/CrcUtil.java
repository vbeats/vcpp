package com.csf.vcpp.utils;

import cn.hutool.core.io.checksum.crc16.CRC16Modbus;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CrcUtil {

	/**
	 * 计算 CRC16-Modbus 校验值（小端序：低字节在前，高字节在后）
	 *
	 * @param data 待校验数据
	 * @return CRC16 校验值（小端序 int）
	 */
	public static int crc16(byte[] data) {
		CRC16Modbus modbus = new CRC16Modbus();
		modbus.update(data);
		int value = (int) modbus.getValue(); // 原始大端值，如 0xAABB
		// 交换高低字节 → 小端序: 0xBBAA
		return ((value & 0xFF) << 8) | ((value >> 8) & 0xFF);
	}

	/**
	 * 校验 CRC16
	 *
	 * @param data     待校验数据
	 * @param expected 期望的 CRC16 值（小端序）
	 * @return 校验是否通过
	 */
	public static boolean checkCrc(byte[] data, int expected) {
		return crc16(data) == (expected & 0xFFFF);
	}
}
