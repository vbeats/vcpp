package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x96 密钥更新（V2.1，密钥长度 + 密钥 + 执行控制由平台附加数据携带）
 */
@Service
@ProtocolCmd(value = FrameType.KEY_UPDATE, protocols = {V21})
public class YkcV21KeyUpdateDownCmd extends YkcPlatformDownCmd {

	public YkcV21KeyUpdateDownCmd() {
		super(FrameType.KEY_UPDATE, false, false);
	}
}
