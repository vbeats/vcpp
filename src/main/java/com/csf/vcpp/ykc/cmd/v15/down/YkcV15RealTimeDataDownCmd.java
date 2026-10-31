package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x12 运营平台自动下发（桩上传 0x13 实时监测数据后的自动下发）
 */
@Service
@ProtocolCmd(value = FrameType.REAL_TIME_DATA, protocols = {V15, V16, V17, V18, V21})
public class YkcV15RealTimeDataDownCmd extends YkcPlatformDownCmd {

	public YkcV15RealTimeDataDownCmd() {
		super(FrameType.REAL_TIME_DATA, true, true);
	}
}
