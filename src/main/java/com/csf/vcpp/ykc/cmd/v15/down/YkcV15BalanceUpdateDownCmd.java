package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x42 余额更新（物理卡号 8B + 余额 4B 由平台附加数据携带）
 */
@Service
@ProtocolCmd(value = FrameType.BALANCE_UPDATE, protocols = {V15, V16, V17, V18, V21})
public class YkcV15BalanceUpdateDownCmd extends YkcPlatformDownCmd {

	public YkcV15BalanceUpdateDownCmd() {
		super(FrameType.BALANCE_UPDATE, false, false);
	}
}
