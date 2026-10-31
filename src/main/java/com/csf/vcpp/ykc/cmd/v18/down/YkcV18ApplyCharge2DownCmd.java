package com.csf.vcpp.ykc.cmd.v18.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V18;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0xA6 申请启动充电(二)回复确认（V1.8+）
 */
@Service
@ProtocolCmd(value = FrameType.APPLY_CHARGE2_ACK, protocols = {V18, V21})
public class YkcV18ApplyCharge2DownCmd extends YkcPlatformDownCmd {

	public YkcV18ApplyCharge2DownCmd() {
		super(FrameType.APPLY_CHARGE2_ACK, true, true);
	}
}
