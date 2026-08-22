---
name: ycpp-parser
description: 解析和生成云快充（云快充/YKC）或瞬峰（瞬峰/Shunfeng）充电桩十六进制协议报文。只要用户提供十六进制报文、询问任意帧类型字段、要求检查 CRC，或要求生成登录、心跳、实时数据、BMS、启停、交易、计费、离线卡、设置、维护、并充、二维码、故障、VIN、密钥、日志等模拟报文，就使用此 skill；完整支持云快充 V1.4、V1.5、V1.6、V1.7、V1.8.1、V2.1 和瞬峰 V1.5 已定义帧，并保留未知字段以便扩展。
---

# ycpp-parser

使用 `scripts/cli.py` 做确定性的报文解析和模拟报文生成，不要凭记忆手工推算字段偏移、长度或 CRC。脚本只使用 Python 标准库，不修改项目 Java 源码。

## 先确定输入模式

- 用户给出报文时使用 `parse`。
- 用户要求“生成”“模拟”“构造”“生成交易记录/登录/计费模型”时使用 `generate`。
- 协议必须明确为 `ykc`/`云快充` 或 `shunfeng`/`瞬峰`。
- 版本必须明确。没有版本时不要根据长度、帧类型或内容猜测：交互终端询问版本；非交互调用让脚本返回 `version_required`。
- 云快充版本统一为 `V1.4`、`V1.5`、`V1.6`、`V1.7`、`V1.8.1`、`V2.1`；用户说“2.x”时应让其确认具体版本，当前内置定义为 V2.1。
- 在用户只说“生成模拟帧”但未给指令时，先使用 `list` 展示该版本的完整帧类型目录，不要任意猜测指令。

## 解析报文

```bash
python .claude/skills/ycpp-parser/scripts/cli.py parse \
  --protocol ykc --version V1.7 \
  --hex "68 ..." --format report --emoji
```

也可以用 `--input path/to/frame.hex`。输入允许紧凑十六进制、空格、换行和 `0x` 前缀。

默认中文报告必须包含协议、版本、方向、帧类型、序列号、字段值和 CRC 结果。`--emoji` 只增加 `⬆️/⬇️` 等装饰，不改变字段名称或顺序。需要机器处理时使用 `--format json`，结果结构见 `schemas/frame-report.schema.json`。

报告中的 CRC 必须显示传输值、计算值和是否匹配。CRC 使用 CRC-16/Modbus，覆盖从 sequence 到消息体的数据区，不包含 `0x68`、长度字段和 CRC 自身；线上字节顺序为低字节在前，和项目 `CrcUtil.java`/`YkcAckUtil.java` 的实际报文行为一致。

## 生成模拟报文

自然语言请求可直接传给 `--request`：

```bash
python .claude/skills/ycpp-parser/scripts/cli.py generate \
  --request "生成云快充1.7版本的交易记录" \
  --fields-json '{"device_no":"55031412782305","gun_no":1}' \
  --format json
```

也可以显式指定：

```bash
python .claude/skills/ycpp-parser/scripts/cli.py generate \
  --protocol shunfeng --version V1.5 --frame-type 0x0C \
  --fields-json request.json --format report
```

生成器在未提供字段时使用零值或明确的模拟占位值，并标记 `generated: true`。生成后脚本会重新解析生成帧，因此长度、字段边界和 CRC 应闭环。当前不伪造 AES/RSA 密文；若要求加密生成，应说明需要真实会话密钥。需要查看某版本全部可生成指令时先执行：

```bash
python .claude/skills/ycpp-parser/scripts/cli.py list \
  --protocol ykc --version V2.1 --format report
```

所有参考文档中定义的帧类型都已注册生成器。字段通过 `--fields-json` 传入：

- 普通定长字段按解析报告中的 `key` 传值；BCD 标识用数字字符串，BIN 数值用十进制数，时间用 ISO 格式。
- 变长内容直接传文本，如 `qr_content`、`qr_url`、`qr_prefix`、`latest_key`，长度字段自动计算并校验。
- 离线卡等重复项使用数组，例如 `{"cards":[{"logical_card":"1234","physical_card":7}]}`。
- 位域、MD5、电表密文等不应臆造含义的定长字段使用十六进制字符串。
- 只有文档字段本身存在无法消除的矛盾时，才使用 `raw_body_hex` 提供整个消息体；仍由脚本封装长度、序列号和 CRC。

## 内置协议差异

- 云快充 V1.4–V1.8.1：`68 + 1 字节数据长度 + sequence(2) + encrypt(1) + type(1) + body + CRC(2)`。
- 云快充 V2.1：`68 + 2 字节数据长度 + sequence(2) + CP56Time2a 发送时间(7) + encrypt(1) + type(1) + body + CRC(2)`；长度按参考文档使用 big-endian，具体变体可在 `frame.py` 的 `EnvelopeSpec` 扩展。
- 云快充登录 `0x01`：V1.4–V1.7 为基础字段；V1.8.1 增加 88 字节随机密钥、3 字节版本和 Token；V2.1 再增加手机号、网络制式和经纬度。
- 云快充交易记录：V1.4 为 `0x3F` 且电表起止值 4 字节；V1.5/V1.6 为 `0x3B` 且为 5 字节；V1.7/V1.8.1 为 `0x3D` 并增加电表号/密文/协议/加密方式；V2.1 为 `0x3D`，使用费率时段数量和可变费率尾部。
- 云快充计费模型：`0x05/0x06` 验证请求/应答，`0x09/0x0A` 模型请求/应答，`0x57/0x58` 设置应答/设置；V1.x 使用固定尖峰平谷四组费率，V2.1 使用最多 48 组可变费率。
- 云快充各版参考文档中的心跳、实时/BMS、启停、离线卡、工作参数、对时、地锁、远程维护帧均可生成；V1.6+ 支持并充，V1.5/V1.6 支持各自二维码补充帧，V2.1 还支持故障、交易召唤、启动完成、平台连接、VIN、密钥和日志帧。
- 云快充 V2.1 经纬度线上值分别为 `(经度+180)*1e6` 和 `(纬度+90)*1e6`；生成器负责偏移和六位精度转换。
- 瞬峰 V1.5：`0x0B` 计费模型请求，`0x0C` 计费模型（8 组费率、计损比例、48 个半小时费率号），`0x81` 交易结算记录（固定 48 个半小时电量）。

字段资料来自本目录 `references/`，但参考 Markdown 存在 OCR 表格、样例长度和 CRC 说明不一致。遇到无法确认的字段，输出原始十六进制和 warning，不要把猜测当成解析值。

## 扩展协议

公共帧、CRC 和字段 codec 在 `scripts/frame.py`、`scripts/crc.py`、`scripts/codecs.py`；版本和命令注册在 `scripts/protocols.py`/`scripts/registry.py`。添加其它厂商协议时注册新的 `(protocol, version, frame_type)` 解码器/编码器，未知帧仍应保留完整 `unknown_body_hex`，不要复用不兼容版本的字段布局。

## 常用验证

```bash
python -m unittest discover -s .claude/skills/ycpp-parser/tests -v
python .claude/skills/ycpp-parser/scripts/cli.py parse --protocol ykc --version V1.5 --hex "68 ..." --format json
```

验证重点：紧凑和空格输入等价、长度错误被拒绝、CRC 修改后显示不匹配、V1.x/V2.1 帧头选择正确、V1.4/1.5/1.7 交易字段长度不同、瞬峰 48 个时段完整保留、未知帧不丢 body、生成报文可再次解析。
