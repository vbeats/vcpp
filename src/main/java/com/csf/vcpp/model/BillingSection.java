package com.csf.vcpp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@ToString
public class BillingSection implements Serializable { // 电费 服务费定义
	@Serial
	private static final long serialVersionUID = -404177995612984092L;

	private BigDecimal ep; // 电价费率

	private BigDecimal sp; // 服务费费率
}
