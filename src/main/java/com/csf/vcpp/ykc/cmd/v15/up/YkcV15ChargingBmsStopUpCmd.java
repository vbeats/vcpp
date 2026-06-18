package com.csf.vcpp.ykc.cmd.v15.up;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcExceptionUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.CHARGING_BMS_STOP, protocols = {V15, V16, V17})
public class YkcV15ChargingBmsStopUpCmd extends YkcCmdExecutor { // 充电阶段BMS中止
	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf body = data.getMsgBody();

		byte[] tradeNoBytes = new byte[16];
		body.readBytes(tradeNoBytes);
		String tradeNo = BcdUtil.toString(tradeNoBytes);  // 交易流水号

		byte[] deviceIdBytes = new byte[7];
		body.readBytes(deviceIdBytes);
		Long deviceId = BcdUtil.bcdBytesToLong(deviceIdBytes);  // 桩号

		byte gunNo = body.readByte();// 枪号

		// BMS 中止充电原因
		String stopReasons = YkcExceptionUtil.parseBmsStopReasons(body.readByte());

		// BMS 中止充电故障原因
		byte[] faultBytes = new byte[2];
		body.readBytes(faultBytes);
		String faultReasons = YkcExceptionUtil.parseBmsFaultReasons(faultBytes);

		// BMS 中止充电错误原因
		String errorReasons = YkcExceptionUtil.parseBmsErrorReasons(body.readByte());

		log.info("""
				【云快充】{} ⬆️ 充电阶段BMS中止 - 交易流水号: {}
				桩号: {}
				枪号: {}
				中止原因: {}
				故障原因: {}
				错误原因: {}
				---------------------------------
				""",
			versionLabel, tradeNo, deviceId, gunNo, stopReasons, faultReasons, errorReasons);


	}
}
