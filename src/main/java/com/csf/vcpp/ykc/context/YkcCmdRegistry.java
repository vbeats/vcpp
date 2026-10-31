package com.csf.vcpp.ykc.context;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.enums.YkcVersion;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.schema.YkcFrameSchemaRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 云快充指令执行器注册表。
 * <p>
 * 不再依赖「包扫描」，改为直接从 Spring 容器取所有 {@link YkcCmdExecutor} Bean，
 * 读取 {@link ProtocolCmd} 注解注册到 {@link YkcServerContext#EXECUTER_MAP}，
 * 因此指令类放在哪个包都不会漏注册。
 * </p>
 * <p>
 * 注册完成后会输出「版本 - 帧类型」覆盖情况，未实现的帧类型与版本对应关系错误都会被显式告警。
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class YkcCmdRegistry implements InitializingBean {

	private final ApplicationContext applicationContext;

	private final YkcFrameSchemaRegistry schemaRegistry;

	/**
	 * 注册时机：本 Bean 初始化时（各 TCP 服务 Bootstrap 通过 @DependsOn 保证在其之后启动），
	 * 而不是容器全部单例就绪后——避免某个 Bean（如 MQTT 客户端）初始化阻塞/失败导致执行器表为空。
	 */
	@Override
	public void afterPropertiesSet() {
		Map<String, YkcCmdExecutor> beans = applicationContext.getBeansOfType(YkcCmdExecutor.class);

		Map<String, String> owners = new TreeMap<>();
		List<String> conflicts = new ArrayList<>();

		for (Map.Entry<String, YkcCmdExecutor> entry : beans.entrySet()) {
			YkcCmdExecutor executor = entry.getValue();
			ProtocolCmd annotation = AnnotatedElementUtils.findMergedAnnotation(executor.getClass(), ProtocolCmd.class);
			if (annotation == null) {
				log.warn("【云快充】 cmd执行器 {} 未标注 @ProtocolCmd, 已忽略", executor.getClass().getSimpleName());
				continue;
			}

			FrameType frameType = annotation.value();
			for (String protocol : annotation.protocols()) {
				if (!YkcVersion.isSupported(protocol)) {
					log.error("【云快充】 {} 声明了未知协议版本: {}", executor.getClass().getSimpleName(), protocol);
					continue;
				}
				String key = protocol + "_" + frameType.getType();
				String previous = owners.putIfAbsent(key, executor.getClass().getSimpleName());
				if (previous != null) {
					conflicts.add(key + " (已注册: " + previous + ", 冲突: " + executor.getClass().getSimpleName() + ")");
					continue;
				}
				YkcServerContext.EXECUTER_MAP.put(key, executor);
			}
		}

		if (!conflicts.isEmpty()) {
			conflicts.forEach(c -> log.error("【云快充】 帧执行器注册冲突: {}", c));
		}

		// 声明式帧结构（独立执行器优先，不会被覆盖）
		schemaRegistry.registerInto(YkcServerContext.EXECUTER_MAP, owners);

		reportCoverage(owners);
	}

	/**
	 * 输出各版本帧类型覆盖情况
	 */
	private void reportCoverage(Map<String, String> owners) {
		for (YkcVersion version : YkcVersion.values()) {
			List<String> missing = new ArrayList<>();
			for (FrameType frameType : version.getSupportedFrames()) {
				String key = version.getLabel() + "_" + frameType.getType();
				if (!owners.containsKey(key)) {
					missing.add(String.format("0x%02X(%s)", frameType.getType(), frameType.name()));
				}
			}
			missing.sort(null);

			// 注册了但版本不支持的帧（版本对应关系错误）
			List<String> invalid = new ArrayList<>();
			for (Map.Entry<String, String> entry : owners.entrySet()) {
				if (!entry.getKey().startsWith(version.getLabel() + "_")) {
					continue;
				}
				int code = Integer.parseInt(entry.getKey().substring(entry.getKey().indexOf('_') + 1));
				FrameType frameType = FrameType.getByValue((short) code);
				if (frameType == null || !version.supports(frameType)) {
					invalid.add(String.format("0x%02X(%s)", code, frameType == null ? "未知" : frameType.name()));
				}
			}

			if (missing.isEmpty() && invalid.isEmpty()) {
				log.info("【云快充】 {} 帧类型覆盖检查通过, 共 {} 个帧类型", version.getLabel(), version.getSupportedFrames().size());
			} else {
				if (!missing.isEmpty()) {
					log.warn("【云快充】 {} 未实现的帧类型({}): {}", version.getLabel(), missing.size(), String.join(", ", missing));
				}
				if (!invalid.isEmpty()) {
					log.warn("【云快充】 {} 版本不支持却注册了执行器的帧类型: {}", version.getLabel(), String.join(", ", invalid));
				}
			}
		}
	}

	/**
	 * 按版本统计已注册帧类型数量（便于运维查看）
	 */
	public Map<YkcVersion, Integer> registeredCount() {
		Map<YkcVersion, Integer> result = new EnumMap<>(YkcVersion.class);
		for (YkcVersion version : YkcVersion.values()) {
			long count = YkcServerContext.EXECUTER_MAP.keySet().stream()
					.filter(k -> k.startsWith(version.getLabel() + "_"))
					.count();
			result.put(version, (int) count);
		}
		return result;
	}
}
