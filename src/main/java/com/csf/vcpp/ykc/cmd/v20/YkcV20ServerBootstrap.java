package com.csf.vcpp.ykc.cmd.v20;

import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.cmd.AbstractYkcServerBootstrap;
import com.csf.vcpp.ykc.router.YkcDataRouter;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V20;

@Service
public class YkcV20ServerBootstrap extends AbstractYkcServerBootstrap {

	public YkcV20ServerBootstrap(AppConfig appConfig, YkcDataRouter ykcDataRouter) {
		super(appConfig, ykcDataRouter);
	}

	@Override
	protected int getPort() {
		return appConfig.getYkc20Port();
	}

	@Override
	protected String getVersionLabel() {
		return V20;
	}
}
