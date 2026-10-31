package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x5B 二维码设置（V2.1，二维码码制 + 长度 + 内容由平台附加数据携带）
 */
@Service
@ProtocolCmd(value = FrameType.QR_SETTING, protocols = {V21})
public class YkcV21QrSettingDownCmd extends YkcPlatformDownCmd {

	public YkcV21QrSettingDownCmd() {
		super(FrameType.QR_SETTING, false, true);
	}
}
