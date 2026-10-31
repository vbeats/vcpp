package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0xF0 后台远程下发二维码前缀（V1.5 补充协议）
 */
@Service
@ProtocolCmd(value = FrameType.QR_PREFIX, protocols = {V15, V16})
public class YkcV15QrPrefixDownCmd extends YkcPlatformDownCmd {

	public YkcV15QrPrefixDownCmd() {
		super(FrameType.QR_PREFIX, false, false);
	}
}
