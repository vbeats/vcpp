package com.csf.vcpp.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
@Setter
@ConfigurationProperties(prefix = "vcpp")
public class AppConfig {

	private String api;  // api接口地址

	private Integer ykc15Port; // ykc1.5 监听端口
	private Integer ykc16Port; // ykc1.6 监听端口
	private Integer ykc17Port; // ykc1.7 监听端口
	private Integer ykc18Port; // ykc1.8 监听端口
	private Integer ykc21Port; // ykc2.1 监听端口

	/**
	 * 计费模型默认费率段数：4 = 尖峰平谷，5 = 尖峰平谷深谷
	 */
	private Integer ykcBillingSectionCount = 4;

	/**
	 * 读空闲判定离线秒数
	 */
	private Integer ykcReaderIdleSeconds = 30;

	/**
	 * 预期同时在线的充电桩数量，仅用于设置「桩号 -> Channel」映射的初始容量。
	 * <p>
	 * 设置过大不会提升性能，只会白白常驻一张空表（例如 100 万 -> 8 MB 数组）；
	 * 设置过小也没有问题：ConcurrentHashMap 的扩容是多线程协作迁移，成本分摊到插入线程，
	 * 不会 stop-the-world。默认 65536（约 0.5 MB 表）。
	 * </p>
	 */
	private Integer ykcExpectedDevices = 65536;
}
