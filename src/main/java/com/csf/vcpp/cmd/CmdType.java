package com.csf.vcpp.cmd;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CmdType {

	LOGIN("LOGIN", "登录应答"),

	BILLING_MODEL("BILLING_MODEL", "下发电价模型"),
	RESTART("RESTART", "远程重启"),
	TIME_SYNC("TIME_SYNC", "对时设置"),

	START_CHARGE("START_CHARGE", "启动充电"),
	PARALLEL_CHARGE("PARALLEL_CHARGE", "并充充电"),
	STOP_CHARGE("STOP_CHARGE", "停止充电"),
	;

	@JsonValue
	private final String type;
	private final String desc;
}
