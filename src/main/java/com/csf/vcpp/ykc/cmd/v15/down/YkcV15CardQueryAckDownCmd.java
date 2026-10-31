package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x48 卡数据查询回复确认
 */
@Service
@ProtocolCmd(value = FrameType.CARD_QUERY_ACK, protocols = {V15, V16, V17, V18, V21})
public class YkcV15CardQueryAckDownCmd extends YkcPlatformDownCmd {

	public YkcV15CardQueryAckDownCmd() {
		super(FrameType.CARD_QUERY_ACK, false, false);
	}
}
