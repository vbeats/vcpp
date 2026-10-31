package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x4E 充电机启动完成应答（V2.1）
 */
@Service
@ProtocolCmd(value = FrameType.CHARGE_MOTOR_START_ACK, protocols = {V21})
public class YkcV21ChargeMotorStartAckDownCmd extends YkcPlatformDownCmd {

	public YkcV21ChargeMotorStartAckDownCmd() {
		super(FrameType.CHARGE_MOTOR_START_ACK, true, true);
	}
}
