package com.csf.vcpp.ykc.utils;

import com.google.common.base.Joiner;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class YkcExceptionUtil {  // 充电桩异常信息

	// ----------- 实时监测数据异常信息 -----------
	public static List<String> getRealTimeDataError(byte[] exception) {
		List<String> es = new ArrayList<>();

		boolean[] faults = new boolean[14];

		// 读取每个比特并设置到布尔数组中
		for (int i = 0; i < 14; i++) {
			// 计算对应的字节和比特位置
			int byteIndex = i / 8; // 字节索引
			int bitIndex = i % 8;  // 比特索引

			// 使用位运算检查该比特位
			faults[i] = ((exception[byteIndex] >> bitIndex) & 1) == 1; // 如果为 1 则故障

			if (faults[i]) {
				es.add(realTimeDataErrorDesc[i]);
			}
		}

		return es;
	}

	private static final String[] realTimeDataErrorDesc = {
		"急停按钮动作故障",
		"无可用整流模块",
		"出风口温度过高",
		"交流防雷故障",
		"交直流模块 DC20 通信中断",
		"绝缘检测模块 FC08 通信中断",
		"电度表通信中断",
		"读卡器通信中断",
		"RC10 通信中断",
		"风扇调速板故障",
		"直流熔断器故障",
		"高压接触器故障",
		"门打开"
	};

	// ----------- 充电阶段BMS 中止异常信息 -----------
	@Getter
	private enum BmsStopReasonEnum {
		SOC_TARGET("需求SOC目标值") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "未达到所需SOC目标值";
					case 1 -> "达到所需SOC目标值";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		TOTAL_VOLTAGE("达到总电压设定值") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "未达到总电压设定值";
					case 1 -> "达到总电压设定值";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		CELL_VOLTAGE("达到单体电压设定值") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "未达到单体电压设定值";
					case 1 -> "达到单体电压设定值";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		CHARGER_INITIATED("充电机主动中止") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "充电机主动中止";
					case 1 -> "充电机中止(收到CST帧)";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		};

		private final String description;

		BmsStopReasonEnum(String description) {
			this.description = description;
		}

		public abstract String getStateDescription(int state);
	}

	public static String parseBmsStopReasons(byte reasonByte) {
		List<String> reasons = new ArrayList<>();

		int value = reasonByte & 0xFF;

		for (BmsStopReasonEnum reason : BmsStopReasonEnum.values()) {
			int bitPosition = reason.ordinal() * 2;
			int mask = 0b11 << bitPosition;
			int groupValue = (value & mask) >>> bitPosition;
			reasons.add(reason.getStateDescription(groupValue));
		}
		return Joiner.on(",").join(reasons);
	}

	@Getter
	public enum BmsFaultReasonsEnum {
		INSULATION_FAULT("绝缘故障") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "绝缘正常";
					case 1 -> "绝缘故障";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		CONNECTOR_OVERHEAT("输出连接器过温故障") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "输出连接器正常";
					case 1 -> "输出连接器过温故障";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		BMS_COMPONENT_OVERHEAT("BMS元件过温故障") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "BMS元件正常";
					case 1 -> "BMS元件过温故障";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		CHARGING_CONNECTOR_FAULT("充电连接器故障") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "充电连接器正常";
					case 1 -> "充电连接器故障";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		BATTERY_OVERHEAT("电池组温度过高故障") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "电池组温度正常";
					case 1 -> "电池组温度过高故障";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		HIGH_VOLTAGE_RELAY_FAULT("高压继电器故障") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "高压继电器正常";
					case 1 -> "高压继电器故障";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		VOLTAGE_DETECTION_FAULT("检测点2电压检测故障") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "检测点2电压检测正常";
					case 1 -> "检测点2电压检测故障";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		OTHER_FAULT("其他故障") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "其他正常";
					case 1 -> "其他故障";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		};

		private final String description;

		BmsFaultReasonsEnum(String description) {
			this.description = description;
		}

		public abstract String getStateDescription(int state);

	}

	public static String parseBmsFaultReasons(byte[] faultBytes) {
		List<String> faults = new ArrayList<>();

		int value = ((faultBytes[0] & 0xFF) << 8) | (faultBytes[1] & 0xFF);

		for (BmsFaultReasonsEnum fault : BmsFaultReasonsEnum.values()) {
			int bitPosition = fault.ordinal() * 2;
			int mask = 0b11 << bitPosition;
			int groupValue = (value & mask) >>> bitPosition;
			faults.add(fault.getStateDescription(groupValue));
		}
		return Joiner.on(", ").join(faults);
	}

	@Getter
	public enum BmsErrorReasonsEnum {
		CURRENT_OVERFLOW("电流过大") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "电流正常";
					case 1 -> "电流超过需求值";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		},
		VOLTAGE_ABNORMAL("电压异常") {
			@Override
			public String getStateDescription(int state) {
				return switch (state) {
					case 0 -> "电压正常";
					case 1 -> "电压异常";
					case 2 -> "不可信状态";
					default -> "未知状态";
				};
			}
		};

		private final String description;

		BmsErrorReasonsEnum(String description) {
			this.description = description;
		}

		public abstract String getStateDescription(int state);

	}

	public static String parseBmsErrorReasons(byte errorByte) {
		List<String> errors = new ArrayList<>();
		int value = errorByte & 0xFF;

		for (BmsErrorReasonsEnum error : BmsErrorReasonsEnum.values()) {
			int bitPosition = error.ordinal() * 2;
			int mask = 0b11 << bitPosition;
			int groupValue = (value & mask) >>> bitPosition;
			errors.add(error.getStateDescription(groupValue));
		}
		return Joiner.on(", ").join(errors);
	}

	// ----------------充电阶段中止充电原因----------------
	public static String parseMotorStopReason(byte b) {
		List<String> reasons = new ArrayList<>();

		if ((b & 0x01) != 0) {
			reasons.add("达到充电机设定的条件中止");
		}
		if ((b & 0x04) != 0) {
			reasons.add("人工中止");
		}
		if ((b & 0x10) != 0) {
			reasons.add("异常中止");
		}
		if ((b & 0x40) != 0) {
			reasons.add("BMS主动中止");
		}

		return Joiner.on(",").join(reasons);
	}

	public static String parseMotorFaultReason(byte[] bytes) {
		List<String> faults = new ArrayList<>();
		int code = ((bytes[0] & 0xFF) << 8) | (bytes[1] & 0xFF);

		if ((code & 0x0003) != 0) {
			faults.add("充电机过温故障");
		}
		if ((code & 0x000C) != 0) {
			faults.add("充电连接器故障");
		}
		if ((code & 0x0030) != 0) {
			faults.add("充电机内部过温故障");
		}
		if ((code & 0x00C0) != 0) {
			faults.add("所需电量不能传送");
		}
		if ((code & 0x0300) != 0) {
			faults.add("充电机急停故障");
		}
		if ((code & 0x0C00) != 0) {
			faults.add("其他故障");
		}

		return Joiner.on(",").join(faults);
	}

	public static String parseMotorErrorReason(byte b) {
		List<String> reasons = new ArrayList<>();

		if ((b & 0x01) != 0) {
			reasons.add("电流不匹配");
		}
		if ((b & 0x04) != 0) {
			reasons.add("电压异常");
		}

		return Joiner.on(",").join(reasons);
	}
}
