# vcpp — AI 协作指南

> 充电桩（EVSE）边缘设备接入服务。向下通过 **Netty TCP** 对接充电桩主板，向上通过 **MQTT v5** 与运营平台通信。
> 当前已实现 **云快充（ykc）1.5 / 1.6 / 1.7 / 1.8 / 2.1** 多版本协议；OCPP 1.6 ~ 2.x 为规划中（见 `doc/OCPP.md`）。

---

## 1. 技术栈与构建

| 项 | 值 |
| --- | --- |
| JDK | 21（使用虚拟线程 `Thread.ofVirtual()`） |
| 框架 | Spring Boot 4.1.2（parent，依赖版本统一在 `pom.xml` 的 `<properties>` 中维护，均为最新正式版） |
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
    ├── context/YkcServerContext.java  执行器表 & 桩号/Channel 映射（ChannelGroup + AttributeKey）
    ├── context/YkcSession.java        单条连接的会话元数据（挂在 Channel attr 上）
    ├── context/YkcCmdRegistry.java    执行器注册 + 版本覆盖检查（唯一注册入口）
    ├── enums/FrameType.java          全部帧类型枚举
    ├── enums/YkcVersion.java         版本能力模型（帧结构参数 + 该版本支持的帧类型集合）
    ├── consts/YkcProtocolConst.java   版本标签常量 V15/V16/V17/V18/V21
    ├── executor/YkcCmdExecutor.java   指令执行器抽象基类
    ├── executor/YkcPlatformDownCmd.java 平台下发帧基类（桩号/枪号 + 附加数据）
    ├── executor/YkcUpAckCmd.java      上行请求 -> 平台确认帧基类
    ├── schema/FrameField.java         声明式字段描述（BCD/ASCII/小端整数/CP56/位标志）
    ├── schema/FrameSchema.java        消息体结构 + 解析器
    ├── schema/YkcFrameSchemaRegistry.java 解析型上行帧统一注册（避免逐帧重复代码）
    ├── model/YkcMsgData.java          帧数据载体（帧头 + 消息体 + ack 参数）
    ├── YkcCodecConfig.java            编解码可选项装配
    ├── utils/                         YkcAckUtil（组应答帧）/ YkcExceptionUtil / YkcBillingModelCodec
    └── cmd/
        ├── AbstractYkcServerBootstrap.java  TCP 服务生命周期（不再做包扫描）
        ├── v15/ v16/ v17/ v18/ v21/        各版本 Bootstrap（每版本一个端口）
        └── v15/up/ v15/down/ v17/up/ v18/up/ v18/down/ v21/up/ v21/down/  上行/下行指令实现
upstream/UpstreamPublisher.java         上行数据上报抽象（MQTT 上行能力预留）
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

`YkcCmdRegistry#afterPropertiesSet()` 是**唯一注册入口**：从 Spring 容器取所有 `YkcCmdExecutor` Bean，
读取 `@ProtocolCmd` 注解，按 `版本标签 + "_" + 帧类型数值` 注册到 `YkcServerContext.EXECUTER_MAP`：

```java
@ProtocolCmd(value = FrameType.LOGIN, protocols = {V15, V16, V17})
```

→ 生成 key：`V15_1`、`V16_1`、`V17_1`

- **不再依赖包扫描**，指令类放在哪个包都会被注册（各 Bootstrap 通过 `@DependsOn("ykcCmdRegistry")` 保证注册先于端口监听）。
- 注册后会输出各版本「未实现的帧类型」和「版本不支持却注册了执行器」的告警；
  `YkcFrameCoverageTest` 把这项检查固化为回归测试，`mvn test` 会因缺帧而失败。
- 版本与帧类型的对应关系集中在 `YkcVersion#getSupportedFrames()`，改版本能力只需改这一处。
- 纯「解析 + 记录 + 上报」的帧（BMS 过程帧、各类设置应答等）无需写执行器，
  在 `YkcFrameSchemaRegistry` 中用 `FrameSchema` 声明字段即可；独立执行器优先，不会被覆盖。

### 3.3 上行链路（桩 → 平台）

```
Netty 读事件 (ByteBuf，先 retain 再交异步线程)
  -> YkcChannelInboundHandler#channelRead0  （提交到虚拟线程执行器，信号量限流防 OOM）
  -> YkcUpDataRouter#route                  （拆帧、CRC 校验、构建 YkcMsgData）
  -> YkcUpDataRouter#routeMsg               （按 versionLabel_frameType 取执行器）
  -> YkcCmdExecutor#execute                 （解析业务字段、落日志、必要时下发应答）
  -> finally: 释放 msgBody ByteBuf 与整帧 ByteBuf
```

- **ByteBuf 生命周期**：拆帧器产出的 ByteBuf 在 `channelRead0` 返回后由 Netty 自动释放；业务在虚拟线程异步执行，
  因此先 `retain()` 再交给 `YkcUpDataRouter`，由其 `finally` 统一释放。所有异常/CRC 失败路径同样释放。
- 读空闲（默认 30s，`vcpp.ykc-reader-idle-seconds`）判定离线，`YkcServerContext.unbind` 移除映射并关闭连接。
- 上行数据经 `upstream/UpstreamPublisher` 上报，当前默认实现只打日志（MQTT 上行能力预留）。

### 3.4 下行链路（平台 → 桩）

```
平台发布 JSON 到 topic: vcpp/cmd
  -> MqttCmdListener#messageArrived
  -> MqttMsgService#handleMsg            （JSON -> CmdMsg）
  -> 按 protocol 分派（目前仅 "ykc"）
  -> 桩号查 YkcServerContext#getChannel 拿 Channel（设备离线则直接丢弃）
  -> CmdType -> FrameType 映射
  -> 按 versionLabel_frameType 取执行器
  -> covertCmdData2YkcMsgData(cmdMsg) 转 YkcMsgData
  -> execute(channel, versionLabel, data) -> YkcAckUtil.createAck -> YkcAckUtil.write
```

> `YkcAckUtil.createAck` 会**接管并在组包结束后释放**传入的 `msgBody`，调用方不得再使用或重复释放。

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
- 计费模型（0x0A/0x58）报文布局：`桩编号(7) + 计费模型编号(2) + [费率数量(1)，仅 V2.1] + 费率段(8×N) + 计损比例(1) + 48时段电价(48)`，编解码统一在 `YkcBillingModelCodec`
  - V1.5 ~ V1.8：N 由消息体长度反推，4 段 = 90 字节；**深谷方案** 5 段（尖/峰/平/谷/**深谷(4)**）= 98 字节，已支持（见 `云快充V1.5-增加深谷（云电）_方案.md`）
  - V2.1：N 取自「费率数量」字段，最多 48 段
  - 默认费率段数由 `vcpp.ykc-billing-section-count`（4 或 5）配置，见 `YkcDefaultBillingModelProvider`
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

4. **下行指令**：用 `Unpooled.buffer(n)` 拼消息体 → `YkcAckUtil.createAck(version, sequence, encryptFlag, frameType, msgBody)`（或基类 `sendAck`）→ `YkcAckUtil.write(channel, res)`。
   - 平台主动下发的帧：直接继承 `YkcPlatformDownCmd`，只需声明 `super(帧类型, 是否含交易流水号, 是否含枪号)`，其余业务字段由平台 JSON 追加。
   - 「上行请求 → 平台确认」的帧：继承 `YkcUpAckCmd`，解析固定头后直接调用下行确认帧执行器。
   - 只需解析上报、无需业务决策的帧：**不要写类**，在 `YkcFrameSchemaRegistry` 加一条 `FrameSchema` 声明即可。
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
  唯一例外：执行器**替换**了 `data.setMsgBody(...)`（如 `YkcUpAckCmd`）时，必须自己先释放原消息体。
  下行帧的消息体交给 `YkcAckUtil.createAck` 后即转移所有权，不得再使用。
- 桩号类型为 **String**（14 位），历史提交中已从 Long 迁移，勿回退。
- 协议实现放在 `ykc/` 下，与协议无关的能力放 `utils/`、`http/`；`cmd/` 只放平台指令模型。
- 提交信息：简短中文短语 + 可选 Gitmoji（参考 `git log`，如 `【云快充】多版本基础通信`、`VIN过滤`）。

---

## 6. 关键扩展点 / 现状说明

- **登录校验**：`YkcV15LoginUpCmd` 中 `boolean loginValid = true;` 为硬编码占位，真实校验应走 `http/ApiClient#login`。
- **上行数据上报（能力预留）**：指令执行器统一通过 `upstream/UpstreamPublisher`（门面 `UpstreamPublisherGateway`）上报，
  默认实现 `NoopUpstreamPublisher` 只打调试日志；接入 MQTT（`vcpp/data|ops|trade/{桩号}`）时新增一个实现 Bean 即可，协议解析代码无需改动。
  平台下行 MQTT 同理暂不实现：`VcppMqttConfig` 连接失败不再 `System.exit(-1)`，只打印错误，保证 broker 不可用时设备接入仍可用。
- **OCPP**：目录与文档已备（`doc/OCPP.md`、`doc/T／CEC 102.x`），代码尚未开始；实现时应参照 `ykc/` 的分层（Bootstrap / Router / Executor / FrameType）在 `ocpp/` 下平行扩展，不要混入 `ykc` 包。
- **连接上下文**（`YkcServerContext`，面向几十万连接）：单索引 `桩号 -> Channel` + `ChannelGroup`（含未登录连接，关闭时自动移除）
  + Channel 上的 `YkcSession` attr。解绑用 `remove(deviceNo, channel)` 条件移除；同一桩号重连会主动关闭旧连接；
  `closeAll()` / `closeByVersion()` 用于停机与单端口下线，`Application` 注册了 JVM 关闭钩子兜底。当前为单实例部署，多实例需改为 Redis 等外部存储。
- **映射容量与内存实测**（JDK 21 / G1 / 压缩指针）：单个条目约 **100 字节**（`Node` 32B + `String` key 56B + 摊到槽位 4B + value 引用），
  故 100 万在线桩 → **约 110 MB**，其中 `Node[] table` 只占 8 MB（2^21 槽 × 4B）。
  真正的内存大头是 Netty 侧（`NioSocketChannel` + pipeline + 缓冲，约 5~20 KB/连接，100 万 → 数 GB），Map 不是瓶颈。
  **初容量不要写死成 100 万**：`new ConcurrentHashMap<>(1<<20)` 会按 0.75 负载因子放大成 2^21 槽，
  第一条连接进来就常驻 10 MB（实测：100 条与 100 万条时都是 10150 KB / 111727 KB，那 10 MB 全程白占），
  且是 G1 humongous 对象直进老年代。而预分配的收益≈0——CHM 扩容由插入线程协作迁移（非 stop-the-world），
  实测 100 万条时预分配版 111.7 MB vs 自然扩容版 113.2 MB，终态基本持平。
  现改为 `vcpp.ykc-expected-devices`（默认 65536 → 约 0.5 MB），由 `YkcCodecConfig` 在启动期调用
  `YkcServerContext.applyExpectedDevices()` 生效；各 Bootstrap 通过 `@DependsOn({"ykcCmdRegistry","ykcCodecConfig"})` 保证先于监听端口。
- **内存泄漏防线**：`Application` 启动时把 `io.netty.leakDetection.level` 默认设为 `advanced`；
  所有 ByteBuf 在 `YkcUpDataRouter` / `YkcAckUtil` / `YkcPlatformDownCmd` / `YkcUpAckCmd` 的 `finally` 或所有权转移处释放；
  虚拟线程派发前有信号量（20 万在途上限）做背压，防止设备突发灌包打爆堆内存。
- **序列号域字节序（已用真实设备日志验证，硬编码，无配置项）**：上行解析用小端（`readUnsignedShortLE`），
  下行用大端下发（`writeShort`），即 **同一序列号值在上下行的 wire 字节顺序相反**——这是桩侧的真实行为，不是 bug。
  `YkcAckUtil#createAck` 中已固定，不要再引入开关：改成小端桩会认为序列号不对。
  验证依据：148 条上行 / 82 条下行真实报文配对，其中 68 组「上行序列号两字节不同」的有鉴别力样本
  **100% 满足「下行字节 = 上行字节反转」**（0 组同序）；旁证是同一连接的上行序列号按 LE 解析连续 +1
  （如 `0x4F0B → 0x4F0C → 0x4F0D → 0x4F0E`），证实上行确为小端。
- **交易记录**：V1.5/V1.6 用 `0x3B`（`YkcV15TradeUpCmd`），V1.7/V1.8 用 `0x3D`（`YkcV17TradeUpCmd`，含电表表号/密文），
  V2.1 用 `0x3D` 但结构不同（`YkcV21TradeUpCmd`，费率时段数量 + 48 时段电量）。深谷（第 5 段）按消息体长度自动识别。
- **深谷（5 段）计费模型已实现**：见 3.6，`vcpp.ykc-billing-section-count=5` 开启。

---

## 7. 参考资料

- 云快充协议原文：`doc/云快充/云快充平台协议V1.5.md` ~ `V2.1.0.md`（含补充文档）
- 测试数据样例：`doc/云快充/测试数据.md`
- 国标参考：`doc/T／CEC 102.1—2016` ~ `102.4—2016` PDF
- OCPP 规划：`doc/OCPP.md`
- 项目概览与端口/主题速查：`README.md`
- 配套充电桩模拟器（Go + go-zero，client 端）：`vcpp-emulator/AGENTS.md`、`vcpp-emulator/README.md`
