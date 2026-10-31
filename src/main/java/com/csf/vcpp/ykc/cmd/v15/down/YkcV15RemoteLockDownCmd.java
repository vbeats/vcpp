package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x62 遥控地锁升锁与降锁命令
 */
@Service
@ProtocolCmd(value = FrameType.REMOTE_LOCK, protocols = {V15, V16, V17, V18, V21})
public class YkcV15RemoteLockDownCmd extends YkcPlatformDownCmd {

	public YkcV15RemoteLockDownCmd() {
		super(FrameType.REMOTE_LOCK, false, true);
	}
}
