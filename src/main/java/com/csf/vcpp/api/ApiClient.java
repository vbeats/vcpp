package com.csf.vcpp.api;

import com.csf.vcpp.model.R;
import com.dtflys.forest.annotation.Address;
import com.dtflys.forest.annotation.JSONBody;
import com.dtflys.forest.annotation.Post;

import java.util.Map;

@Address(basePath = "#{app.api}")
public interface ApiClient {

	@Post(url = "/login")
	R login(@JSONBody Map<String, Object> params);

	@Post("/getBillingModel")
	R getBillingModel(@JSONBody Map<String, Object> params);
}
