package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15ApplyChargeDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcUpAckCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x31 申请启动充电（桩侧刷卡 / VIN 申请）→ 平台回复确认 0x32
 */
@Service
@ProtocolCmd(value = FrameType.APPLY_CHARGE, protocols = {V15, V16, V17, V18, V21})
public class YkcV15ApplyChargeUpCmd extends YkcUpAckCmd {

	public YkcV15ApplyChargeUpCmd(YkcV15ApplyChargeDownCmd applyChargeDownCmd) {
		super(FrameType.APPLY_CHARGE, applyChargeDownCmd, true);
	}
}
