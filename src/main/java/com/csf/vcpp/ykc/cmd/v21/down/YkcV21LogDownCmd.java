package com.csf.vcpp.ykc.cmd.v21.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0x98 日志召唤（V2.1，FTP 地址/端口/用户名/密码/路径/时间等由平台附加数据携带）
 */
@Service
@ProtocolCmd(value = FrameType.LOG, protocols = {V21})
public class YkcV21LogDownCmd extends YkcPlatformDownCmd {

	public YkcV21LogDownCmd() {
		super(FrameType.LOG, false, false);
	}
}
