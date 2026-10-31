package com.csf.vcpp;

import com.csf.vcpp.ykc.context.YkcServerContext;
import io.netty.util.internal.SystemPropertyUtil;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

	/**
	 * Netty 内存泄漏检测级别：
	 * simple(默认) 抽样检测；advanced 抽样 + 报告最近访问位置；paranoid 全量检测（仅压测环境使用）。
	 */
	private static final String LEAK_DETECTION_KEY = "io.netty.leakDetection.level";

	public static void main(String[] args) {
		if (SystemPropertyUtil.get(LEAK_DETECTION_KEY) == null) {
			System.setProperty(LEAK_DETECTION_KEY, "advanced");
		}

		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			// 兜底：确保 JVM 退出前关闭全部设备连接并释放映射
			YkcServerContext.closeAll();
		}, "vcpp-shutdown-hook"));

		SpringApplication.run(Application.class, args);
	}
}
