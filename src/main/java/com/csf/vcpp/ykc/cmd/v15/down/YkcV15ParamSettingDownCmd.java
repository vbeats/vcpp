package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x52 充电桩工作参数设置
 */
@Service
@ProtocolCmd(value = FrameType.PARAM_SETTING, protocols = {V15, V16, V17, V18, V21})
public class YkcV15ParamSettingDownCmd extends YkcPlatformDownCmd {

	public YkcV15ParamSettingDownCmd() {
		super(FrameType.PARAM_SETTING, false, true);
	}
}
