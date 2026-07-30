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
| 命例生命周期 | `CaseLifecycleUseCase` + `CaseRepository` | 详情、回收站、复制 | 页面直接删行或复制附件引用 |
| 重复候选 | `CaseRepository.findDuplicateCandidates` | 新建、编辑 | 只按姓名自动合并或静默覆盖 |
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
| 非空库恢复与密码备份恢复 | 不存在 | 待设计 | 禁止静默降级 |

## Stage 3A 增量入口矩阵

| 入口/消费者 | 当前状态 | 唯一入口 | 最小验证 |
|---|---|---|---|
| 编辑身份与出生资料 | 已接通，保存前按期望修订号读取并重新计算 | `EditCaseUseCase` → `BaziEngine.calculate` → `CaseRepository.save` | 旧修订在重算前拒绝；旧快照保留、新快照采用 |
| 文本记录增改删 | 已支持笔记、反馈、师傅点评、统一分类分析及版本历史 | `TextRecordUseCase` → `CaseRepository.save` | 身份、创建时间、分类、顺序和新增/修改/删除历史 |
| 关键事件增改删 | 已支持可选标题、八类类别、未知/年/月/日精度、状态及版本历史 | `CaseEventUseCase` → `CaseRepository.save` | 标题、类别、日期校验、精度、顺序和旧数据基线补建 |
| 整例删除 | 已由后续生命周期增量补齐软删除与恢复 | `CaseLifecycleUseCase` | 禁止 DAO 物理删除 |
| 备份/恢复 | 精确保真通道 | `CaseBackupService` | 新增记录和事件仍由 Stage 1 全量回归覆盖 |

## Stage 3A 生命周期增量入口矩阵

| 入口/消费者 | 当前状态 | 唯一入口 | 最小验证 |
|---|---|---|---|
| 整例删除与恢复 | 软删除进入回收站，完整聚合和附件引用保留 | `CaseLifecycleUseCase` → `CaseRepository.save` | 主列表隐藏、回收站可见、恢复后事实不变 |
| 命例复制 | 生成新稳定 ID，只复制出生资料、分类与计算快照 | `CaseLifecycleUseCase.duplicate` | 来源 ID、新快照 ID、空记录/附件和别名去重 |
| 重复命例提示 | 新建与编辑在保存前按出生身份和四柱生成候选 | `CaseRepository.findDuplicateCandidates` | 活动/回收站位置、理由、确认前零写入 |
| 完整备份与旧版恢复 | Schema v5 精确保真软删除、复制来源和记录历史 | `CaseBackupService` | 旧备份默认空历史、v1→v5 迁移和往返 |

## 变更门禁

下列变更必须同步更新规则文档、黄金样本和测试：

- 年界、月界、子初换日、起运算法；
- 公历/农历/真太阳时归一化；
- 引擎版本或规则版本；
- 结果字段含义。

## Stage 3B 数据交换入口矩阵

| 入口/消费者 | 当前状态 | 唯一入口 | 最小验证 |
|---|---|---|---|
| 单命例 JSON 导出 | v1 严格封套、明文风险确认或密码保护、载荷/文件哈希和系统创建文档 | `MainActivity` SAF → `StageTwoViewModel` → `SingleCaseExchangeService.export` | 聚合往返、真实明文/加密文件、密码不落库且不降级 |
| 单命例只读预览 | 系统打开文档、明文 16 MiB/容器 24 MiB 上限、解密后格式/Schema/哈希/领域校验和预览弹窗 | `MainActivity` SAF → `StageTwoViewModel` → `SingleCaseExchangeService.preview` | 同一真实加密文件输错/输对密码、冲突显示且数据库零写入 |
| 单命例密码容器 | 保护版本 1；PBKDF2-HMAC-SHA256 600,000 次导出；AES-256-GCM、16 字节盐、12 字节 nonce、128 位 tag 与参数 AAD | `SingleCaseEncryption`，只由 `SingleCaseExchangeService` 调用 | 无密码、错密码、篡改和越界 KDF 参数统一安全拒绝；正确密码恢复原严格封套 |
| 完整备份导出 | 明文风险确认后写出 ZIP，或把 ZIP 流式写入独立 v1 密码容器 `.nfbak` | `MainActivity` SAF → `StageTwoViewModel` → `CaseBackupService.export` | 真实系统 ZIP/密码容器、manifest/文件哈希与导出计数 |
| 完整备份只读预览 | 系统打开文档；密码文件先认证解密到私有临时 ZIP，再校验路径/大小/哈希/引用/附件并清理 | `MainActivity` SAF → `StageTwoViewModel` → `CaseBackupService.preview` | 无密码/错密码/篡改安全拒绝；非空当前库仍零写入并显示计数 |
| 完整备份密码容器 | 独立保护版本 1；PBKDF2-HMAC-SHA256 600,000 次、AES-256-GCM 与参数 AAD；不整包入内存 | `BackupEncryption`，只由 `CaseBackupService` 调用 | 参数边界、认证失败、明文不可见及 Android `.nfbak` 往返 |
| 本地冲突候选 | 稳定 ID、出生输入、四柱和回收站位置 | `CaseRepository` 查询，由预览服务合并 | 同一候选理由合并且不修改本地命例 |
| 无附件命例提交 | 跳过零写入；保留两份重建全部聚合 ID，提交前复查冲突 | `StageTwoViewModel` → `SingleCaseExchangeService.commitImport` → `CaseRepository.save` | 过期预览拒绝、事务失败不覆盖、系统文件 E2E 后列表读回第二份 |
| 范围化合并 | 模块仅追加独有内容；标量差异逐字段采用，默认本地 | `prepareMerge` 固定目标 revision/载荷 → `commitMerge` 复查 → `CaseRepository.save` | 内容去重、子项重建 ID、目标变化拒绝、模拟器记录/事件合并 |
| 附件二进制 | 单 JSON 不携带，仅保留引用元数据；存在引用时禁止提交 | `REFERENCES_ONLY` | 预览明确限制并返回 `ATTACHMENT_BINARIES_REQUIRED` |
