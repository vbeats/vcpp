package com.csf.vcpp.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BinUtil {

	public static byte[] int2Bin(Integer bin) {
		byte[] bytes = new byte[4];

		bytes[0] = (byte) (bin & 0xFF);
		bytes[1] = (byte) ((bin >> 8) & 0xFF);
		bytes[2] = (byte) ((bin >> 16) & 0xFF);
		bytes[3] = (byte) ((bin >> 24) & 0xFF);

		return bytes;
	}

	public static int bin2int(byte[] bytes) {
		int value = 0;
		for (int i = 0; i < bytes.length; i++) {
			value |= (bytes[i] & 0xFF) << 8 * i;
		}
		return value;
	}


	// 5字节bytes转long
	public static long bin2Long(byte[] bytes) {
		// 使用小端字节序读取 5 字节数字
		int byte1 = bytes[0] & 0xFF;
		int byte2 = bytes[1] & 0xFF;
		int byte3 = bytes[2] & 0xFF;
		int byte4 = bytes[3] & 0xFF;
		int byte5 = bytes[4] & 0xFF;

		// 将读取的字节合并成一个 long 值
		return ((long) byte1) |
			((long) byte2 << 8) |
			((long) byte3 << 16) |
			((long) byte4 << 24) |
			((long) byte5 << 32);
	}
}
