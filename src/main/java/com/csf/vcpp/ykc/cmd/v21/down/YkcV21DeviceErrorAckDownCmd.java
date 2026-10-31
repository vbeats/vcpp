package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x49 设备故障确认（V2.1）
 */
@Service
@ProtocolCmd(value = FrameType.DEVICE_ERROR_ACK, protocols = {V21})
public class YkcV21DeviceErrorAckDownCmd extends YkcPlatformDownCmd {

	public YkcV21DeviceErrorAckDownCmd() {
		super(FrameType.DEVICE_ERROR_ACK, false, true);
	}
}
