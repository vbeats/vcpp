package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.cmd.v15.down.YkcV15ParallelChargeDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcUpAckCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0xA1 并充模式申请启动充电 → 平台回复确认 0xA2
 */
@Service
@ProtocolCmd(value = FrameType.PARALLEL_CHARGE, protocols = {V15, V16, V17, V18, V21})
public class YkcV15ParallelChargeUpCmd extends YkcUpAckCmd {

	public YkcV15ParallelChargeUpCmd(YkcV15ParallelChargeDownCmd parallelChargeDownCmd) {
		super(FrameType.PARALLEL_CHARGE, parallelChargeDownCmd, true);
	}
}
