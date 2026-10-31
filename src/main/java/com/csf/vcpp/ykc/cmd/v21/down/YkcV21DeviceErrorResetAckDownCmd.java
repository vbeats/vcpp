package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x4A 设备故障复位确认（V2.1）
 */
@Service
@ProtocolCmd(value = FrameType.DEVICE_ERROR_RESET_ACK, protocols = {V21})
public class YkcV21DeviceErrorResetAckDownCmd extends YkcPlatformDownCmd {

	public YkcV21DeviceErrorResetAckDownCmd() {
		super(FrameType.DEVICE_ERROR_RESET_ACK, false, true);
	}
}
