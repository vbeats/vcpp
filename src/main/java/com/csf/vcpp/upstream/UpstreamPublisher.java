package com.csf.vcpp.upstream;

/**
 * 上行数据上报能力抽象（能力预留）。
 * <p>
 * 设备侧上行数据（状态 / 运营 / 订单）最终需要送到上层平台，
 * 传输介质可能是 MQTT（{@code vcpp/data/{桩号}}、{@code vcpp/ops/{桩号}}、{@code vcpp/trade/{桩号}}）、
 * Kafka 或数据库。当前阶段<b>暂不实现</b>具体通道，仅预留统一入口，
 * 指令执行器通过本接口上报，后续接入真实通道时无需改动协议解析代码。
 * </p>
 */
public interface UpstreamPublisher {

	/**
	 * 设备状态 / 一般数据：{@code vcpp/data/{桩号}}
	 */
	void publishData(String deviceNo, String event, Object payload);

	/**
	 * 登录 / 电价等运营信息：{@code vcpp/ops/{桩号}}
	 */
	void publishOps(String deviceNo, String event, Object payload);

	/**
	 * 订单：{@code vcpp/trade/{桩号}}
	 */
	void publishTrade(String deviceNo, String event, Object payload);
}
