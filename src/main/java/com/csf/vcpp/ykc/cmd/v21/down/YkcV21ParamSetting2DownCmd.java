package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x5F 参数设置(二)（V2.1）
 */
@Service
@ProtocolCmd(value = FrameType.PARAM_SETTING2, protocols = {V21})
public class YkcV21ParamSetting2DownCmd extends YkcPlatformDownCmd {

	public YkcV21ParamSetting2DownCmd() {
		super(FrameType.PARAM_SETTING2, false, true);
	}
}
