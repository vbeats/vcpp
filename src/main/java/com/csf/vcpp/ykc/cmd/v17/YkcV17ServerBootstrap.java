package com.csf.vcpp.ykc.cmd.v17;

import com.csf.vcpp.config.AppConfig;
import com.csf.vcpp.ykc.cmd.AbstractYkcServerBootstrap;
import com.csf.vcpp.ykc.router.YkcDataRouter;
import org.springframework.stereotype.Service;

import static com.csf.vcpp.ykc.consts.YkcProtocolConst.V17;

@Service
public class YkcV17ServerBootstrap extends AbstractYkcServerBootstrap {

	public YkcV17ServerBootstrap(AppConfig appConfig, YkcDataRouter ykcDataRouter) {
		super(appConfig, ykcDataRouter);
	}

	@Override
	protected int getPort() {
		return appConfig.getYkc17Port();
	}

	@Override
	protected String getVersionLabel() {
		return V17;
	}
}
