package com.csf.vcpp.ykc.cmd.v18.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V18;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x60 默认最大功率下发（V1.8+）
 */
@Service
@ProtocolCmd(value = FrameType.MAX_POWER, protocols = {V18, V21})
public class YkcV18MaxPowerDownCmd extends YkcPlatformDownCmd {

	public YkcV18MaxPowerDownCmd() {
		super(FrameType.MAX_POWER, false, true);
	}
}
