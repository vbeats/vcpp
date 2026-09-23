# vcpp — AI 协作指南

> 充电桩（EVSE）边缘设备接入服务。向下通过 **Netty TCP** 对接充电桩主板，向上通过 **MQTT v5** 与运营平台通信。
> 当前已实现 **云快充（ykc）1.5 / 1.6 / 1.7 / 1.8 / 2.1** 多版本协议；OCPP 1.6 ~ 2.x 为规划中（见 `doc/OCPP.md`）。

---

## 1. 技术栈与构建

| 项 | 值 |
| --- | --- |
| JDK | 21（使用虚拟线程 `Thread.ofVirtual()`） |
| 框架 | Spring Boot 4.0.6（parent） |
| 构建 | Maven（`pom.xml`，`finalName=vcpp`） |
| 网络 | Netty 4.2.x（`netty-all`） |
| MQTT | Eclipse Paho MQTT v5 |
| HTTP 客户端 | Forest 声明式客户端 |
| 工具库 | Hutool、fastjson2、Guava、commons-codec |
| 其他 | Lombok（`@Slf4j` / `@Getter` / `@RequiredArgsConstructor`） |

常用命令（Windows PowerShell）：

```powershell
mvn -q compile                       # 编译
mvn test                             # 单元测试（JUnit5 + spring-boot-starter-test）
mvn -q package -DskipTests           # 打包 -> target/vcpp.jar
mvn spring-boot:run                  # 本地启动（默认 dev profile）
java -jar target/vcpp.jar            # 运行
```

- 主类：`com.csf.vcpp.Application`
- 配置：`src/main/resources/application.yml`（公共）+ `application-dev.yml`（本地）
- 日志：`src/main/resources/logback-spring.xml`

### 端口约定

| 用途 | 端口 | 配置项 |
| --- | --- | --- |
| Web | 10086 | `server.port` |
| 云快充 1.5 | 9847 | `vcpp.ykc15-port` |
| 云快充 1.6 | 9848 | `vcpp.ykc16-port` |
| 云快充 1.7 | 9849 | `vcpp.ykc17-port` |
| 云快充 1.8 | 9850 | `vcpp.ykc18-port` |
| 云快充 2.1 | 9851 | `vcpp.ykc21-port` |

> 云快充 1.4 已淘汰，不提供监听端口。

---

## 2. 目录结构

```
src/main/java/com/csf/vcpp/
├── Application.java              启动类
├── annotation/ProtocolCmd.java   协议指令注册注解（帧类型 + 支持版本）
├── cmd/
│   ├── CmdMsg.java               平台下行指令消息体（MQTT JSON）
│   └── CmdType.java              平台指令枚举（LOGIN/BILLING_MODEL/...）
├── config/AppConfig.java         vcpp.* 配置绑定
├── mqtt/
│   ├── VcppMqttConfig.java       MQTT 客户端 & 订阅 vcpp/cmd
│   ├── MqttCmdListener.java      MQTT 回调入口
│   └── MqttMsgService.java       下行指令分发（CmdType -> FrameType -> Executor）
├── http/ApiClient.java           Forest 声明式 HTTP（设备登录校验等）
├── model/                        BillingModel / BillingSection / R
├── utils/                        BcdUtil / BinUtil / CP56Time2aUtil / CrcUtil
└── ykc/                          云快充协议实现
    ├── YkcChannelHandler.java        Netty pipeline 装配
    ├── YkcChannelInboundHandler.java 连接生命周期 + 虚拟线程派发
    ├── router/YkcUpDataRouter.java   上行报文拆帧、CRC 校验、路由
    ├── context/YkcServerContext.java  执行器表 & 桩号/Channel 映射
    ├── enums/FrameType.java          全部帧类型枚举
    ├── consts/YkcProtocolConst.java   版本标签常量 V15/V16/V17/V18/V21
    ├── executor/YkcCmdExecutor.java   指令执行器抽象基类
    ├── model/YkcMsgData.java          帧数据载体（帧头 + 消息体 + ack 参数）
    ├── utils/                         YkcAckUtil（组应答帧）/ YkcExceptionUtil（故障解析）
    └── cmd/
        ├── AbstractYkcServerBootstrap.java  TCP 服务 + 指令扫描注册
        ├── v15/ v16/ v17/ v18/ v21/        各版本 Bootstrap（每版本一个端口）
        └── v15/up/ v15/down/ v18/up/ v21/up/  上行/下行指令实现
doc/云快充/*.md   各版本协议原文（改动协议实现前必读）
```

---

## 3. 核心机制

### 3.1 报文帧结构

```
[0x68 起始符 1B] [数据长度] [序列号 2B LE] [发送时间 7B(仅 v2.1+)] [加密标志 1B] [帧类型 1B] [消息体 nB] [CRC16 2B]
```

- 数据长度：v2.1+ 为 **2 字节**，其余版本为 **1 字节**
- 发送时间：仅 v2.1+，CP56Time2a 编码（`CP56Time2aUtil`）
- 加密标志：`0x00` 不加密 / `0x01` DES 加密
- CRC：CRC16-Modbus，**小端序**（低字节在前），见 `CrcUtil` / `YkcAckUtil`
- 拆帧：`LengthFieldBasedFrameDecoder(65540, 1, lenFieldLength, adj, 0, true)`，见 `YkcChannelHandler`
- 桩号：**7 字节 BCD**，对应 14 位十进制桩号（`BcdUtil.bcdBytesToHex`）

### 3.2 指令注册表（核心约定）

`AbstractYkcServerBootstrap.initCmdExecutor()` 在启动时扫描**自身所在包及其子包**下带 `@ProtocolCmd` 的类，
按 `版本标签 + "_" + 帧类型数值` 注册到 `YkcServerContext.EXECUTER_MAP`：

```java
@ProtocolCmd(value = FrameType.LOGIN, protocols = {V15, V16, V17})
```

→ 生成 key：`V15_1`、`V16_1`、`V17_1`

**因此：新增指令类必须放在对应版本 Bootstrap 的包（或其子包）下**，否则不会被扫描到。
例如 V21 的指令必须放在 `com.csf.vcpp.ykc.cmd.v21.**`。

### 3.3 上行链路（桩 → 平台）

```
Netty 读事件 (ByteBuf -> HEX 字符串)
  -> YkcChannelInboundHandler#channelRead0  （提交到虚拟线程执行器）
  -> YkcUpDataRouter#route                  （拆帧、CRC 校验、构建 YkcMsgData）
  -> YkcUpDataRouter#routeMsg               （按 versionLabel_frameType 取执行器）
  -> YkcCmdExecutor#execute                 （解析业务字段、落日志、必要时下发应答）
  -> finally: 释放 msgBody ByteBuf
```

- 读空闲 30s 判定离线，清理 `DEVICE_CHANNEL_MAP` / `CHANNEL_DEVICE_MAP` 并关闭连接。
- 上行数据当前以**结构化日志**输出，尚未接 Kafka/DB/MQTT 上报（`MqttMsgService` 目前只处理下行）。

### 3.4 下行链路（平台 → 桩）

```
平台发布 JSON 到 topic: vcpp/cmd
  -> MqttCmdListener#messageArrived
  -> MqttMsgService#handleMsg            （JSON -> CmdMsg）
  -> 按 protocol 分派（目前仅 "ykc"）
  -> 桩号查 DEVICE_CHANNEL_MAP 拿 Channel（设备离线则直接丢弃）
  -> CmdType -> FrameType 映射
  -> 按 versionLabel_frameType 取执行器
  -> covertCmdData2YkcMsgData(cmdMsg) 转 YkcMsgData
  -> execute(channel, versionLabel, data) -> YkcAckUtil.createAck -> channel.writeAndFlush
```

下行消息格式（平台侧）：

```json
{ "protocol": "ykc", "versionLabel": "V18", "cmdType": "START_CHARGE", "deviceNo": "桩号", "data": {} }
```

已实现的平台指令：`LOGIN`、`BILLING_MODEL`、`RESTART`、`TIME_SYNC`、`START_CHARGE`、`PARALLEL_CHARGE`、`STOP_CHARGE`。

### 3.5 平台 MQTT Topic 约定

| 方向 | Topic |
| --- | --- |
| 平台 ⬇️ 指令 | `vcpp/cmd` |
| 设备 ⬆️ 状态 / 一般数据 | `vcpp/data/{桩号}` |
| 设备 ⬆️ 登录 / 电价等运营信息 | `vcpp/ops/{桩号}` |
| 设备 ⬆️ 订单 | `vcpp/trade/{桩号}` |

### 3.6 计费模型（价格模型）

- `BillingSection`：有序集合，尖峰平谷模式下顺序为 **尖(0) → 峰(1) → 平(2) → 谷(3)**，含电费率 `ep` / 服务费率 `sp`
- `BillingModel`：48 个时段，每段 `start`/`end`（HH:mm）+ `sectionIndex` 指向 `BillingSection`
- 下发/应答时费率按 `× 100000` 取整（`RoundingMode.DOWN`），电量/金额按 `× 0.0001` 还原，单价按 `× 0.00001` 还原
- 计费模型编号固定 `0x0100`

---

## 4. 新增协议指令的标准步骤

以「云快充 V15 新增上行帧 `0xXX`」为例：

1. **确认帧类型**：若 `FrameType` 中不存在，先在 `ykc/enums/FrameType.java` 增加枚举（数值 + 中文说明 + `⬆️/⬇️` 注释）。
2. **创建指令类**：放在 `ykc/cmd/v15/up/`（上行）或 `ykc/cmd/v15/down/`（下行），命名 `YkcV15{业务}{Up|Down}Cmd`。
3. **继承 `YkcCmdExecutor`**，加注解：

```java
@Service
@Slf4j
@RequiredArgsConstructor
@ProtocolCmd(value = FrameType.XXX, protocols = {V15, V16, V17})
public class YkcV15XxxUpCmd extends YkcCmdExecutor {
    private final YkcV15XxxDownCmd xxxDownCmd; // 需要应答时注入下行指令

    @Override
    public void execute(Channel channel, String versionLabel, YkcMsgData data) {
        ByteBuf body = data.getMsgBody();
        // 1) 按协议文档顺序读取字段（BCD / LE / CP56Time2a 注意区分）
        // 2) 打印结构化日志
        // 3) 需要应答时：填充 data 的 ack 参数 -> 调用下行指令 execute
    }
}
```

4. **下行指令**：用 `Unpooled.buffer(n)` 拼消息体 → `YkcAckUtil.createAck(sequence, encryptFlag, frameType, msgBody)` → `channel.writeAndFlush(res)`。
5. **若需平台主动下发**：在 `CmdType` 增加枚举，在 `MqttMsgService#handleYkcCmd` 的 switch 中补充 `CmdType -> FrameType` 映射，并重写 `covertCmdData2YkcMsgData(CmdMsg)`。
6. **若某版本实现与 V15 不一致**（厂商兼容问题）：在对应版本包（如 `v21/up/`）新建同名帧指令类，只标注自己支持的版本，**不要**改 V15 类；跨版本复用可直接注入已有下行指令 Bean（参见 `YkcV21LoginUpCmd` 注入 `YkcV15LoginDownCmd`）。
7. **核对协议文档**：`doc/云快充/云快充平台协议V{版本}.md`，字段长度/字节序以文档为准。

---

## 5. 编码规范（务必遵守）

- **缩进用 Tab**，不使用空格（与现有代码一致）。
- 类注解固定组合：`@Service` + `@Slf4j` + `@RequiredArgsConstructor` + `@ProtocolCmd`，依赖用构造器注入（`final` 字段）。
- 日志前缀统一：`【云快充】{} ⬆️ 业务名` / `【云快充】{} ⬇️ 业务名`，第一个占位符传 `versionLabel`；正文用 Java 文本块（`"""`），结尾跟 `-----` 分隔行。
- 字节序：桩号/时间用 BCD 或 CP56Time2a；数值字段多为 **小端（LE）**，如 `readUnsignedIntLE` / `writeIntLE`，逐字段对齐协议文档。
- 金额/电量一律 `BigDecimal`，禁止 `double` 中间计算。
- `ByteBuf` 取自 `YkcMsgData#getMsgBody()`，由 `YkcUpDataRouter#routeMsg` 的 `finally` 统一释放；指令内部不要再 `release()`。
- 桩号类型为 **String**（14 位），历史提交中已从 Long 迁移，勿回退。
- 协议实现放在 `ykc/` 下，与协议无关的能力放 `utils/`、`http/`；`cmd/` 只放平台指令模型。
- 提交信息：简短中文短语 + 可选 Gitmoji（参考 `git log`，如 `【云快充】多版本基础通信`、`VIN过滤`）。

---

## 6. 关键扩展点 / 现状说明

- **登录校验**：`YkcV15LoginUpCmd` 中 `boolean loginValid = true;` 为硬编码占位，真实校验应走 `http/ApiClient#login`。
- **上行数据上报**：目前仅日志输出，MQTT 上行（`vcpp/data/{桩号}` 等）尚未实现。
- **OCPP**：目录与文档已备（`doc/OCPP.md`、`doc/T／CEC 102.x`），代码尚未开始；实现时应参照 `ykc/` 的分层（Bootstrap / Router / Executor / FrameType）在 `ocpp/` 下平行扩展，不要混入 `ykc` 包。
- **连接上下文为全局静态 Map**（`YkcServerContext`），单实例部署；多实例需改为 Redis 等外部存储。
- `VcppMqttConfig#mqttClient` 连接失败会 `System.exit(-1)`，排查启动即退出问题时优先看这里。

---

## 7. 参考资料

- 云快充协议原文：`doc/云快充/云快充平台协议V1.5.md` ~ `V2.1.0.md`（含补充文档）
- 测试数据样例：`doc/云快充/测试数据.md`
- 国标参考：`doc/T／CEC 102.1—2016` ~ `102.4—2016` PDF
- OCPP 规划：`doc/OCPP.md`
- 项目概览与端口/主题速查：`README.md`
