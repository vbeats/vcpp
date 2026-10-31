package com.csf.vcpp.ykc.cmd.v21.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.cmd.v21.down.YkcV21DeviceErrorResetAckDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcUpAckCmd;
import com.csf.vcpp.ykc.model.YkcMsgData;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x4B 设备故障复位 → 平台确认 0x4A
 */
@Service
@ProtocolCmd(value = FrameType.DEVICE_ERROR_RESET, protocols = {V21})
public class YkcV21DeviceErrorResetUpCmd extends YkcUpAckCmd {

	public YkcV21DeviceErrorResetUpCmd(YkcV21DeviceErrorResetAckDownCmd resetAckDownCmd) {
		super(FrameType.DEVICE_ERROR_RESET, resetAckDownCmd, false);
	}

	@Override
	protected byte[] resultBytes(YkcMsgData data) {
		return new byte[]{0x00}; // 确认结果: 0x00 成功
	}
}
