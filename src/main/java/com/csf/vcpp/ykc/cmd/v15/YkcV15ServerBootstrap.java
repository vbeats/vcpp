package com.csf.vcpp.ykc.cmd.v15;

import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.cmd.AbstractYkcServerBootstrap;
import com.csf.vcpp.ykc.router.YkcDataRouter;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V15;

@Service
public class YkcV15ServerBootstrap extends AbstractYkcServerBootstrap {

	public YkcV15ServerBootstrap(AppConfig appConfig, YkcDataRouter ykcDataRouter) {
		super(appConfig, ykcDataRouter);
	}

	@Override
	protected int getPort() {
		return appConfig.getYkc15Port();
	}

	@Override
	protected String getVersionLabel() {
		return V15;
	}
}
