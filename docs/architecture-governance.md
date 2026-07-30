# 架构与治理

## 业务真相与唯一入口

当前存在两项受治理的业务真相：

1. 同一出生输入和同一计算口径，必须得到可版本追踪、可重复验证的计算结果。
2. 命例、来源证据、记录和附件必须完整保存、按原顺序读取，并能通过版本化协议恢复。

所有计算必须经过：

```text
调用方 → BaziEngine.calculate(input, profile) → 引擎适配器 → 版本化结果
```

任何界面、导入器或数据库代码都不得绕过该入口直接调用 Tyme4j。

## 所有权矩阵

| 概念 | 唯一所有者 | 当前消费者 | 禁止事项 |
|---|---|---|---|
| `BirthInput` | `core:domain` | 引擎适配器 | 用 UI 字符串代替结构化时间 |
| `CalculationProfile` | `core:domain` | 引擎适配器 | 隐式更改换年或起运口径 |
| `CalculationResult` | `core:domain` | 测试；后续仓储 | 只保存展示文本、不保存版本 |
| `BaziEngine` | `core:domain` | 后续用例层 | 多套平行计算入口 |
| Tyme4j 状态隔离 | `core:engine-tyme` | `TymeBaziEngine` | 其他模块访问全局 provider |
| `BaziCase` 与字段空值语义 | `core:domain` | 仓储、备份 | 页面或 OCR 用空串改写真值 |
| 命例增量写入 | `CaseRepository` | 手动录入；未来 OCR | DAO、解析器或页面直接写库 |
| 完整备份与空库恢复 | `CaseBackupService` | 后续设置页 | 无范围覆盖、忽略哈希或静默降级明文 |
| Room Schema 与迁移 | `core:data` | 仓储、恢复 | 破坏性迁移或省略 Schema 证据 |

## 模块边界

- `app`：组合 `core:data` 与 `core:engine-tyme`，承载手动录入用例、ViewModel、
  手机单列导航和展示；不得复制计算或持久化规则。
- `core:domain`：纯 Kotlin 领域模型和端口，不依赖 Android 或第三方历法库。
- `core:engine-tyme`：Tyme4j 适配器及状态隔离。
- `core:data`：Room、仓储实现、JSON/ZIP 协议、恢复校验和附件提交。
- 后续 OCR 模块必须在进入对应阶段后新增，不能提前塞入 `app`。

## Stage 1 入口矩阵

| 入口/消费者 | 当前状态 | 唯一入口 | 最小验证 |
|---|---|---|---|
| 手动新建、编辑 | 领域与仓储已实现；UI 不存在 | `CaseRepository.save` | 创建、更新、旧修订冲突 |
| 问真截图导入 | 不存在 | 未来只提交 `BaziCase` 草稿 | 本阶段不验收 |
| 完整备份 | 服务已实现；UI 不存在 | `CaseBackupService.export` | JSON、附件、哈希与确定性往返 |
| 完整恢复 | 仅支持空库 | `restoreIntoEmptyStore` | 路径安全、哈希、引用、失败回滚 |
| 列表、详情、搜索 | 仓储查询已实现；UI 不存在 | `CaseRepository` | 别名/姓名检索与事实往返 |
| 真机和系统文件选择 | 不存在 | 不适用 | 本阶段不验收 |

## Stage 2 入口矩阵

| 入口/消费者 | 当前状态 | 唯一入口 | 最小验证 |
|---|---|---|---|
| 手动新建 | 已接通公历民用时最小表单 | `CreateCaseUseCase` → `BaziEngine.calculate` → `CaseRepository.save` | 表单、计算、冲突和异常契约 |
| 命例列表与搜索 | 已显示姓名/别名、性别、出生时间和四柱 | `CaseRepository.search` | Room 查询与 ViewModel 搜索 |
| 命例详情 | 已区分原始录入和计算结果 | `CaseRepository.findById` | 导航与详情读取 |
| 编辑、删除、复制 | 不存在 | 后续仍须经过 `CaseRepository` | 本阶段不验收 |
| 问真截图导入 | 不存在 | 未来输入适配器提交标准草稿 | 本阶段不验收 |
| 完整备份与空库恢复 | 精确保真，不受 UI 接线影响 | `CaseBackupService` | Stage 1 回归测试 |
| 非空库恢复与密码加密 | 不存在 | 待设计 | 禁止静默降级 |

## Stage 3A 增量入口矩阵

| 入口/消费者 | 当前状态 | 唯一入口 | 最小验证 |
|---|---|---|---|
| 编辑身份与出生资料 | 已接通，保存前按期望修订号读取并重新计算 | `EditCaseUseCase` → `BaziEngine.calculate` → `CaseRepository.save` | 旧修订在重算前拒绝；旧快照保留、新快照采用 |
| 文本记录增改删 | 已支持笔记、反馈、师傅点评和分析 | `TextRecordUseCase` → `CaseRepository.save` | 身份、创建时间、顺序和修订回归 |
| 关键事件增改删 | 已支持未知/年/月/日精度与状态 | `CaseEventUseCase` → `CaseRepository.save` | 日期校验、精度和顺序回归 |
| 整例删除 | 不存在 | 待附件删除与恢复策略确认 | 禁止无恢复边界直接删除 |
| 备份/恢复 | 精确保真通道 | `CaseBackupService` | 新增记录和事件仍由 Stage 1 全量回归覆盖 |

## 变更门禁

下列变更必须同步更新规则文档、黄金样本和测试：

- 年界、月界、子初换日、起运算法；
- 公历/农历/真太阳时归一化；
- 引擎版本或规则版本；
- 结果字段含义。
