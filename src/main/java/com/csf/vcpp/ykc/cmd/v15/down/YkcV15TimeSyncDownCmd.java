package com.csf.vcpp.ykc.cmd.v15.down;

import cn.hutool.core.util.HexUtil;
import com.csf.vcpp.annotation.ProtocolCmd;
import com.csf.vcpp.utils.BcdUtil;
import com.csf.vcpp.utils.CP56Time2aUtil;
import com.csf.vcpp.ykc.enums.FrameType;
import com.csf.vcpp.ykc.executor.YkcCmdExecutor;
import com.csf.vcpp.ykc.model.YkcMsgData;
import com.csf.vcpp.ykc.utils.YkcAckUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.*;

@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.TIME_SYNC, protocols = {V15, V16, V17, V18, V20})
public class YkcV15TimeSyncDownCmd extends YkcCmdExecutor {  // 对时设置

	@Override
	public void execute(Channel channel, String versionLabel, YkcMsgData data) {
		ByteBuf msgBody = Unpooled.buffer(14);
		msgBody.writeBytes(data.getDeviceIdBytes());

		LocalDateTime now = LocalDateTime.now();
		msgBody.writeBytes(CP56Time2aUtil.encode(now)); // 当前时间

		log.info("【云快充】{} ⬇️ 对时设置: 桩号 {}, 时间: {}", versionLabel, BcdUtil.bcdBytesToLong(data.getDeviceIdBytes()), now);

		ByteBuf res = YkcAckUtil.createAck(0, data.getEncryptFlag(), FrameType.TIME_SYNC, msgBody);

		log.info("【云快充】{} ⬇️ 对时设置Hex: {}", versionLabel, new String(HexUtil.encodeHex(res.array())).toUpperCase());

		channel.writeAndFlush(res);
	}
}
