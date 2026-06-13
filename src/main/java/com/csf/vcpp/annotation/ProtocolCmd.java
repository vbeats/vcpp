package com.csf.vcpp.annotation;

import com.csf.vcpp.ykc.enums.FrameType;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ProtocolCmd {

	FrameType value(); // frameType  帧类型

	String[] protocols(); // 支持的版本号
}
