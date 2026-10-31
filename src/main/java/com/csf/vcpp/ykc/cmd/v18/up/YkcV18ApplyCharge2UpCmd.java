package com.csf.vcpp.ykc.cmd.v18.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.cmd.v18.down.YkcV18ApplyCharge2DownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcUpAckCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V18;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0xA5 申请启动充电(二) → 平台回复确认 0xA6
 */
@Service
@ProtocolCmd(value = FrameType.APPLY_CHARGE2, protocols = {V18, V21})
public class YkcV18ApplyCharge2UpCmd extends YkcUpAckCmd {

	public YkcV18ApplyCharge2UpCmd(YkcV18ApplyCharge2DownCmd applyCharge2DownCmd) {
		super(FrameType.APPLY_CHARGE2, applyCharge2DownCmd, true);
	}
}
