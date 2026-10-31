package com.csf.vcpp.ykc.cmd.v18.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcPlatformDownCmd;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V18;
import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0xA8 运营平台远程启机(二)（V1.8+）
 */
@Service
@ProtocolCmd(value = FrameType.REMOTE_CHARGE2, protocols = {V18, V21})
public class YkcV18RemoteCharge2DownCmd extends YkcPlatformDownCmd {

	public YkcV18RemoteCharge2DownCmd() {
		super(FrameType.REMOTE_CHARGE2, true, true);
	}
}
