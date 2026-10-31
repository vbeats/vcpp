package com.csf.vcpp.ykc;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.enums.YkcVersion;
import com.csf.vcpp.ykc.schema.YkcFrameSchemaRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 帧类型覆盖回归测试：保证每个协议版本声明支持的帧类型都有对应执行器。
 */
class YkcFrameCoverageTest {

	@Test
	void everySupportedFrameHasExecutor() {
		Set<String> registered = registeredKeys();

		List<String> missing = new java.util.ArrayList<>();
		for (YkcVersion version : YkcVersion.values()) {
			for (FrameType frameType : version.getSupportedFrames()) {
				String key = version.getLabel() + "_" + frameType.getType();
				if (!registered.contains(key)) {
					missing.add(String.format("%s 0x%02X(%s)", key, frameType.getType(), frameType.name()));
				}
			}
		}

		assertTrue(missing.isEmpty(), "以下版本/帧类型缺少执行器: " + String.join(", ", missing));
	}

	@Test
	void noExecutorRegisteredForUnsupportedFrame() {
		Set<String> registered = registeredKeys();

		List<String> invalid = new java.util.ArrayList<>();
		for (YkcVersion version : YkcVersion.values()) {
			for (String key : registered) {
				if (!key.startsWith(version.getLabel() + "_")) {
					continue;
				}
				short code = Short.parseShort(key.substring(key.indexOf('_') + 1));
				FrameType frameType = FrameType.getByValue(code);
				if (frameType == null || !version.supports(frameType)) {
					invalid.add(key + "(0x" + Integer.toHexString(code & 0xFF).toUpperCase() + ")");
				}
			}
		}

		assertTrue(invalid.isEmpty(), "以下帧类型在该版本中不存在却注册了执行器: " + String.join(", ", invalid));
	}

	private Set<String> registeredKeys() {
		Set<String> keys = new HashSet<>();

		ClassPathScanningCandidateComponentProvider scanner =
				new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(ProtocolCmd.class));

		for (BeanDefinition definition : scanner.findCandidateComponents("com.csf.vcpp.ykc.cmd")) {
			try {
				Class<?> clazz = Class.forName(definition.getBeanClassName());
				ProtocolCmd annotation = AnnotatedElementUtils.findMergedAnnotation(clazz, ProtocolCmd.class);
				if (annotation == null) {
					continue;
				}
				for (String protocol : annotation.protocols()) {
					keys.add(protocol + "_" + annotation.value().getType());
				}
			} catch (ClassNotFoundException e) {
				throw new IllegalStateException(e);
			}
		}

		YkcFrameSchemaRegistry registry = new YkcFrameSchemaRegistry(null);
		for (YkcFrameSchemaRegistry.Definition definition : registry.getDefinitions()) {
			for (String protocol : definition.protocols()) {
				keys.add(protocol + "_" + definition.frameType().getType());
			}
		}

		return keys;
	}
}
