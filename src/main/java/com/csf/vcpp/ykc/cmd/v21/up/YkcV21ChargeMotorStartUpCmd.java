package com.csf.vcpp.ykc.cmd.v21.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.cmd.v21.down.YkcV21ChargeMotorStartAckDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcUpAckCmd;
import com.csf.vcpp.ykc.model.YkcMsgData;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x4F 充电机启动完成 → 平台确认 0x4E
 */
@Service
@ProtocolCmd(value = FrameType.CHARGE_MOTOR_START, protocols = {V21})
public class YkcV21ChargeMotorStartUpCmd extends YkcUpAckCmd {

	public YkcV21ChargeMotorStartUpCmd(YkcV21ChargeMotorStartAckDownCmd startAckDownCmd) {
		super(FrameType.CHARGE_MOTOR_START, startAckDownCmd, true);
	}

	@Override
	protected byte[] resultBytes(YkcMsgData data) {
		return new byte[]{0x00}; // 启动结果: 0x00 成功
	}
}
