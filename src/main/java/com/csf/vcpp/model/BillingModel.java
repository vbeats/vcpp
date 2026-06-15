package com.csf.vcpp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class BillingModel implements Serializable { // 价格模型
	@Serial
	private static final long serialVersionUID = 4414841718712433798L;

	private String start; // 开始时间 HH:mm
	private String end; // 结束时间 HH:mm

	private Integer sectionIndex; // 计费区间段索引  尖峰平谷模式 从0开始, 自由定价模式从1开始

}
