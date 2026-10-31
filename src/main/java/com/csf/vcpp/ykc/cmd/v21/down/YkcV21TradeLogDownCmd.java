package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x4D 交易记录召唤（V2.1）
 */
@Service
@ProtocolCmd(value = FrameType.TRADE_LOG, protocols = {V21})
public class YkcV21TradeLogDownCmd extends YkcPlatformDownCmd {

	public YkcV21TradeLogDownCmd() {
		super(FrameType.TRADE_LOG, true, true);
	}
}
