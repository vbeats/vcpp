package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x32 申请启动充电回复确认
 */
@Service
@ProtocolCmd(value = FrameType.APPLY_CHARGE_ACK, protocols = {V15, V16, V17, V18, V21})
public class YkcV15ApplyChargeDownCmd extends YkcPlatformDownCmd {

	public YkcV15ApplyChargeDownCmd() {
		super(FrameType.APPLY_CHARGE_ACK, true, true);
	}
}
