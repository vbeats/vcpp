## vcpp

充电桩 边缘设备服务

- [ ] 云快充 1.5 ~ 2.x
- [ ] OCPP 1.6 ~ 2.x

## 端口

```
web 服务端口：10086

tcp 服务端口:

云快充: 1.4 - 淘汰
云快充: 1.5 - 9847
云快充: 1.6 - 9848
云快充: 1.7 - 9849
云快充: 1.8 - 9850
云快充: 2.1 - 9851
```

## 桩号

云快充: 14位长整数

## 平台端消息通信 MQTT

- 平台⬇️指令 topic: vcpp/cmd

  > 下行指令: {protocol:ykc, versionLabel:V18, cmdType: CmdType, deviceId:桩号, data: 业务数据}

- 设备⬆️状态 / 一般数据 topic: vcpp/data/{充电桩号}
- 设备⬆️登录 / 电价 ... 运营信息: vcpp/ops/{充电桩号}
- 设备⬆️订单 topic: vcpp/trade/{充电桩号}

## 兼容性处理

> 不同厂商主板不一定严格按照标准协议实现 不兼容的地方额外定义 ProtocolCmd.protocols 单独处理

## 报文demo

[云快充](doc/云快充.md)
