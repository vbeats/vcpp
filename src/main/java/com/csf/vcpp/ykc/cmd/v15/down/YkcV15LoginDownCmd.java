package com.csf.vcpp.ykc.cmd.v15.down;

import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.utils.CP56Time2aUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.enums.YkcVersion;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

/**
 * 0x02 登录应答。
 * <p>
 * 消息体：桩编号(7B BCD) + 登录结果(1B) + 当前系统时间(7B CP56Time2a)
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.LOGIN_ACK, protocols = {V15, V16, V17, V18, V21})
public class YkcV15LoginDownCmd extends YkcCmdExecutor { // 登录应答

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf msgBody = Unpooled.buffer(15);
		msgBody.writeBytes(data.getDeviceNoBytes()); // 桩编号
		msgBody.writeByte(data.getLoginRes());        // 登录结果
		msgBody.writeBytes(CP56Time2aUtil.encode(LocalDateTime.now())); // 当前系统时间

		ByteBuf res = YkcAckUtil.createAck(YkcVersion.of(versionLabel), data.getSequence(), data.getEncryptFlag(),
				FrameType.LOGIN_ACK, msgBody);

		if (log.isDebugEnabled()) {
			log.debug("【云快充】{} ⬇️ 登录请求应答 - 桩号: {} Hex: {}", versionLabel,
					BcdUtil.bcdBytesToHex(data.getDeviceNoBytes()), ByteBufUtil.hexDump(res).toUpperCase());
		}

		YkcAckUtil.write(channel, res);
	}
}
