package com.csf.vcpp.ykc.cmd.v18;

import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.cmd.AbstractYkcServerBootstrap;
import com.csf.vcpp.ykc.router.YkcUpDataRouter;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V18;
import org.springframework.context.annotation.DependsOn;

@Service
@DependsOn({"ykcCmdRegistry", "ykcCodecConfig"})
public class YkcV18ServerBootstrap extends AbstractYkcServerBootstrap {

	public YkcV18ServerBootstrap(AppConfig appConfig, YkcUpDataRouter ykcUpDataRouter) {
		super(appConfig, ykcUpDataRouter);
	}

	@Override
	protected int getPort() {
		return appConfig.getYkc18Port();
	}

	@Override
	protected String getVersionLabel() {
		return V18;
	}
}
