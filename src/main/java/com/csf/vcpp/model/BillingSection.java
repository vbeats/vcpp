package com.csf.vcpp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class BillingSection implements Serializable { // 计费区间段

	private BigDecimal ep; // 电价费率

	private BigDecimal sp; // 服务费费率
}
