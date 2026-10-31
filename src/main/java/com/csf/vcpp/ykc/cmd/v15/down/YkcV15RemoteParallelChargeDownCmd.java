package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0xA4 并充模式运营平台远程控制启机
 */
@Service
@ProtocolCmd(value = FrameType.REMOTE_PARALLEL_CHARGE, protocols = {V15, V16, V17, V18, V21})
public class YkcV15RemoteParallelChargeDownCmd extends YkcPlatformDownCmd {

	public YkcV15RemoteParallelChargeDownCmd() {
		super(FrameType.REMOTE_PARALLEL_CHARGE, true, true);
	}
}
