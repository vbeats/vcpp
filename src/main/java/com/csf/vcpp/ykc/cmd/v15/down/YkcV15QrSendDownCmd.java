package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V16;

/**
 * 0x9C 平台下发二维码（V1.6 补充协议）
 */
@Service
@ProtocolCmd(value = FrameType.QR_SEND, protocols = {V16})
public class YkcV15QrSendDownCmd extends YkcPlatformDownCmd {

	public YkcV15QrSendDownCmd() {
		super(FrameType.QR_SEND, false, true);
	}
}
