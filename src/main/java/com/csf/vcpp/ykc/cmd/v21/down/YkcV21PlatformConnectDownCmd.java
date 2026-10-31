package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x5D 平台连接模式设置 / 计费更新（V2.1）
 */
@Service
@ProtocolCmd(value = FrameType.PLATFORM_CONNECT, protocols = {V21})
public class YkcV21PlatformConnectDownCmd extends YkcPlatformDownCmd {

	public YkcV21PlatformConnectDownCmd() {
		super(FrameType.PLATFORM_CONNECT, false, false);
	}
}
