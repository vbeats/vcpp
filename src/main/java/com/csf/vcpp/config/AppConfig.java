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

	private Integer ykc15Port; // ykc1.5 监听端口
	private Integer ykc16Port; // ykc1.6 监听端口
	private Integer ykc17Port; // ykc1.7 监听端口
	private Integer ykc18Port; // ykc1.8 监听端口
	private Integer ykc21Port; // ykc2.1 监听端口
}
