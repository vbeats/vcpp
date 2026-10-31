package com.csf.vcpp.ykc.cmd.v21;

import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.cmd.AbstractYkcServerBootstrap;
import com.csf.vcpp.ykc.router.YkcUpDataRouter;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V21;
import org.springframework.context.annotation.DependsOn;

@Service
@DependsOn({"ykcCmdRegistry", "ykcCodecConfig"})
public class YkcV21ServerBootstrap extends AbstractYkcServerBootstrap {

	public YkcV21ServerBootstrap(AppConfig appConfig, YkcUpDataRouter ykcUpDataRouter) {
		super(appConfig, ykcUpDataRouter);
	}

	@Override
	protected int getPort() {
		return appConfig.getYkc21Port();
	}

	@Override
	protected String getVersionLabel() {
		return V21;
	}
}
