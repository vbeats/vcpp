package com.csf.vcpp.ykc.cmd.v21.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.ykc.cmd.v21.down.YkcV21VinUploadAckDownCmd;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcUpAckCmd;
import com.csf.vcpp.ykc.model.YkcMsgData;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;

/**
 * 0xA9 充电桩上报 VIN 码 → 平台确认 0xAA
 */
@Service
@ProtocolCmd(value = FrameType.VIN_UPLOAD, protocols = {V21})
public class YkcV21VinUploadUpCmd extends YkcUpAckCmd {

	public YkcV21VinUploadUpCmd(YkcV21VinUploadAckDownCmd vinUploadAckDownCmd) {
		super(FrameType.VIN_UPLOAD, vinUploadAckDownCmd, false);
	}

	@Override
	protected byte[] resultBytes(YkcMsgData data) {
		return new byte[]{0x00}; // 设置结果: 0x00 成功
	}
}
