package com.csf.vcpp.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serial;
import java.io.Serializable;

@Getter
@Setter
@ToString
public class R<T> implements Serializable {
	@Serial
	private static final long serialVersionUID = -8032190107505279825L;

	private Integer code;

	private Boolean success;

	private String msg;

	private T data;
}
