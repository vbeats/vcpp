package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x46 离线卡数据清除
 */
@Service
@ProtocolCmd(value = FrameType.CARD_CLEAR, protocols = {V15, V16, V17, V18, V21})
public class YkcV15CardClearDownCmd extends YkcPlatformDownCmd {

	public YkcV15CardClearDownCmd() {
		super(FrameType.CARD_CLEAR, false, false);
	}
}
