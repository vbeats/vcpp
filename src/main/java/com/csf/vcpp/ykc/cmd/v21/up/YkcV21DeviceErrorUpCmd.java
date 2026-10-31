package com.csf.vcpp.ykc.cmd.v21.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.cmd.v21.down.YkcV21DeviceErrorAckDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcUpAckCmd;
import com.csf.vcpp.ykc.model.YkcMsgData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x50 设备故障上送 → 平台确认 0x49
 */
@Slf4j
@Service
@ProtocolCmd(value = FrameType.DEVICE_ERROR, protocols = {V21})
public class YkcV21DeviceErrorUpCmd extends YkcUpAckCmd {

	public YkcV21DeviceErrorUpCmd(YkcV21DeviceErrorAckDownCmd deviceErrorAckDownCmd) {
		super(FrameType.DEVICE_ERROR, deviceErrorAckDownCmd, false);
	}

	@Override
	protected byte[] resultBytes(YkcMsgData data) {
		return new byte[]{0x00}; // 确认结果: 0x00 成功
	}
}
