package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x44 离线卡数据同步（卡数量 + 卡数据由平台附加数据携带）
 */
@Service
@ProtocolCmd(value = FrameType.CARD_SYNC, protocols = {V15, V16, V17, V18, V21})
public class YkcV15CardSyncDownCmd extends YkcPlatformDownCmd {

	public YkcV15CardSyncDownCmd() {
		super(FrameType.CARD_SYNC, false, false);
	}
}
