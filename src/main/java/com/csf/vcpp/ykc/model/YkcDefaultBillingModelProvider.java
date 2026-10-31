package com.csf.vcpp.ykc.model;

import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.model.BillingModel;
import com.csf.vcpp.model.BillingSection;
import com.csf.vcpp.ykc.enums.YkcVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 默认计费模型提供者（平台侧计费模型接口尚未对接，先按配置给出可用默认值）。
 * <p>
 * 费率段数由 {@code vcpp.ykc-billing-section-count} 决定：
 * </p>
 * <ul>
 *     <li>4 = 尖 / 峰 / 平 / 谷</li>
 *     <li>5 = 尖 / 峰 / 平 / 谷 / 深谷（深谷电价扩充方案）</li>
 * </ul>
 * V2.1 段数由报文「费率数量」字段决定，此处最多支持 48 段。
 */
@Service
@RequiredArgsConstructor
public class YkcDefaultBillingModelProvider {

	/**
	 * 电费率默认值：尖 / 峰 / 平 / 谷 / 深谷
	 */
	private static final BigDecimal[] DEFAULT_EP = {
			new BigDecimal("1.20000"), new BigDecimal("1.00000"),
			new BigDecimal("0.80000"), new BigDecimal("0.50000"), new BigDecimal("0.30000")};

	/**
	 * 服务费率默认值：尖 / 峰 / 平 / 谷 / 深谷
	 */
	private static final BigDecimal[] DEFAULT_SP = {
			new BigDecimal("0.40000"), new BigDecimal("0.40000"),
			new BigDecimal("0.40000"), new BigDecimal("0.40000"), new BigDecimal("0.40000")};

	/**
	 * 48 个半小时时段默认归属的费率段（0 尖 1 峰 2 平 3 谷 4 深谷）
	 */
	private static final int[] RATE_48 = {
			3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,          // 00:00 - 06:59
			2, 2,                                              // 07:00 - 07:59
			1, 1, 1, 1, 1, 1, 1,                               // 08:00 - 11:29
			2, 2, 2, 2, 2,                                     // 11:30 - 13:59
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1,                      // 14:00 - 18:59
			0, 0, 0, 0,                                        // 19:00 - 20:59
			1, 1, 1, 1,                                        // 21:00 - 22:59
			3, 3                                               // 23:00 - 23:59
	};

	/**
	 * 深谷模式下 48 时段的归属：00:00 - 05:59 为深谷(4)，06:00 - 06:59 为谷(3)
	 */
	private static final int[] RATE_48_DEEP_VALLEY = {
			4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4,                // 00:00 - 05:59
			3, 3,                                              // 06:00 - 06:59
			2, 2,                                              // 07:00 - 07:59
			1, 1, 1, 1, 1, 1, 1,                               // 08:00 - 11:29
			2, 2, 2, 2, 2,                                     // 11:30 - 13:59
			1, 1, 1, 1, 1, 1, 1, 1, 1, 1,                      // 14:00 - 18:59
			0, 0, 0, 0,                                        // 19:00 - 20:59
			1, 1, 1, 1,                                        // 21:00 - 22:59
			3, 3                                               // 23:00 - 23:59
	};

	private final AppConfig appConfig;

	/**
	 * 默认费率段
	 */
	public List<BillingSection> sections(YkcVersion version) {
		int count = sectionCount(version);
		List<BillingSection> sections = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			BillingSection section = new BillingSection();
			section.setEp(DEFAULT_EP[i % DEFAULT_EP.length]);
			section.setSp(DEFAULT_SP[i % DEFAULT_SP.length]);
			sections.add(section);
		}
		return sections;
	}

	/**
	 * 默认 48 时段电价
	 */
	public List<BillingModel> models(YkcVersion version) {
		int count = sectionCount(version);
		int[] rates = count > 4 ? RATE_48_DEEP_VALLEY : RATE_48;

		List<BillingModel> models = new ArrayList<>(48);
		for (int i = 0; i < 48; i++) {
			BillingModel model = new BillingModel();
			model.setStart(format(i));
			model.setEnd(format(i + 1));
			int index = i < rates.length ? rates[i] : 0;
			model.setSectionIndex(Math.min(index, count - 1));
			models.add(model);
		}
		return models;
	}

	/**
	 * 费率段数
	 */
	public int sectionCount(YkcVersion version) {
		Integer configured = appConfig.getYkcBillingSectionCount();
		int count = configured == null ? 4 : configured;
		count = Math.max(1, Math.min(count, 5));
		// V1.5 ~ V1.8 非深谷模式固定 4 段
		if (!version.isVariableBillingSections() && count < 4) {
			count = 4;
		}
		return count;
	}

	private static String format(int halfHourIndex) {
		int hour = (halfHourIndex / 2) % 24;
		int minute = (halfHourIndex % 2) * 30;
		if (halfHourIndex >= 48) {
			return "24:00";
		}
		return String.format("%02d:%02d", hour, minute);
	}
}
