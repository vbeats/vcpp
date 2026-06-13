package com.csf.vcpp.ykc.cmd.v16;

import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.cmd.AbstractYkcServerBootstrap;
import com.csf.vcpp.ykc.router.YkcDataRouter;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V16;

@Service
public class YkcV16ServerBootstrap extends AbstractYkcServerBootstrap {

	public YkcV16ServerBootstrap(AppConfig appConfig, YkcDataRouter ykcDataRouter) {
		super(appConfig, ykcDataRouter);
	}

	@Override
	protected int getPort() {
		return appConfig.getYkc16Port();
	}

	@Override
	protected String getVersionLabel() {
		return V16;
	}
}
