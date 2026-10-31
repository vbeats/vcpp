package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x94 远程升级（升级地址 / 端口 / 用户名 / 密码 / 版本等由平台附加数据携带）
 */
@Service
@ProtocolCmd(value = FrameType.REMOTE_UPDATE, protocols = {V15, V16, V17, V18, V21})
public class YkcV15RemoteUpdateDownCmd extends YkcPlatformDownCmd {

	public YkcV15RemoteUpdateDownCmd() {
		super(FrameType.REMOTE_UPDATE, false, false);
	}
}
