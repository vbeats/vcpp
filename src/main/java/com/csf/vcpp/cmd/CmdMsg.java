package com.csf.vcpp.cmd;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
public class CmdMsg<T> implements Serializable {

	@Serial
	private static final long serialVersionUID = -513477915493859066L;

	private String protocol; // ykc || .....

	private String versionLabel; // V15 V17 .....

	private int sequence = 0; // 序列号

	private byte encryptFlag = 0x00; // 加密标志 0x00 不加密  0x01 des加密

	private CmdType cmdType; // 平台指令

	private Long deviceId; // 桩号

	private T data;
}
