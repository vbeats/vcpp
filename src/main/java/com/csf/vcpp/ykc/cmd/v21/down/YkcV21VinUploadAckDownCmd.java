package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0xAA 充电桩上报 VIN 码回复确认（V2.1）
 */
@Service
@ProtocolCmd(value = FrameType.VIN_UPLOAD_ACK, protocols = {V21})
public class YkcV21VinUploadAckDownCmd extends YkcPlatformDownCmd {

	public YkcV21VinUploadAckDownCmd() {
		super(FrameType.VIN_UPLOAD_ACK, false, false);
	}
}
