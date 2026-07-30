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
| 真太阳时校正 | `TrueSolarTimeCalculator` + `core:solar-time` | `TymeBaziEngine` | 页面自行加分钟、覆盖原始民用时或把 Tyme 类型名当算法 |
| Tyme4j 状态隔离 | `core:engine-tyme` | `TymeBaziEngine` | 其他模块访问全局 provider |
| `BaziCase` 与字段空值语义 | `core:domain` | 仓储、备份 | 页面或 OCR 用空串改写真值 |
| 命例增量写入 | `CaseRepository` | 手动录入；未来 OCR | DAO、解析器或页面直接写库 |
| 命例生命周期 | `CaseLifecycleUseCase` + `CaseRepository` | 详情、回收站、复制 | 页面直接删行或复制附件引用 |
| 重复候选 | `CaseRepository.findDuplicateCandidates` | 新建、编辑 | 只按姓名自动合并或静默覆盖 |
| 单命例轻量交换 | `SingleCaseExchangeService` | Stage 3B JSON 系统文件入口 | 页面解析 JSON、把引用伪装成附件或绕过两阶段提交 |
| 单命例附件包 | `SingleCaseBundleService` | Stage 3B `.nfbcase` 系统文件入口 | 页面解析 ZIP、跳过来源重读或绕过附件事务 |
| 完整备份与恢复 | `CaseBackupService` | Stage 3B 系统文件入口 | 无范围覆盖、忽略哈希、静默降级明文或页面直接写库 |
| Room Schema 与迁移 | `core:data` | 仓储、恢复 | 破坏性迁移或省略 Schema 证据 |
| 图片导入会话 | `ImportSessionRepository` | 图片入口、后台识别协调器 | OCR、页面或 Worker 直接写 Room |
| 私有导入图片 | `PrivateImportImageStore` | Photo Picker、系统分享入口 | 后台任务长期持有外部 URI 或传递 Bitmap |
| OCR、长图与重复提示 | `core:image-parser` | `ImportRecognitionCoordinator` | 在线引擎进入主链、整张展开超大图、按相似哈希自动合并、单关键词猜测页面或直接写正式命例 |

## 模块边界

- `app`：组合 `core:data` 与 `core:engine-tyme`，承载手动录入用例、ViewModel、
  手机单列导航和展示；不得复制计算或持久化规则。
- `core:domain`：纯 Kotlin 领域模型和端口，不依赖 Android 或第三方历法库。
- `core:solar-time`：NREL SPA 真太阳时适配器，只输出领域证据，不计算四柱。
- `core:engine-tyme`：Tyme4j 适配器及状态隔离。
- `core:data`：Room、仓储实现、JSON/ZIP 协议、恢复校验和附件提交。
- `core:image-parser`：离线 OCR 端口、bundled ML Kit Android 适配器、问真页面分类与
  可恢复识别协调；不依赖 Compose、Room 或八字计算实现。

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
| 手动新建 | 已接通公历/农历/闰月民用时表单 | `CreateCaseUseCase` → `BaziEngine.calculate` → `CaseRepository.save` | 表单、双向转换、计算、冲突和异常契约 |
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
| 完整备份与旧版恢复 | Schema v7 精确保真软删除、复制来源、记录历史和出生时间候选 | `CaseBackupService` | 旧备份默认空历史/空候选、v1→v7 迁移和往返 |

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
| 单命例附件包导出 | `.nfbcase` v1；manifest 绑定 `case.json` 与每个附件的 ID/路径/大小/SHA-256，可明文或密码保护 | `MainActivity` SAF → `StageTwoViewModel` → `SingleCaseBundleService.export` | Android 系统文件明文/加密往返；附件字节与引用完整 |
| 单命例附件包预览/提交 | 私有临时区防御性展开；提交重读同一来源并复核 manifest/文档，重建附件身份及引用 | `SingleCaseBundleService.preview/commitImport/commitMerge` → 共享附件事务 | 零写入预览、错密码拒绝、来源变化拒绝、失败回滚与重试 |
| 单命例附件包密码容器 | 独立 magic/type 的保护版本 1；ZIP 流式进入 PBKDF2-HMAC-SHA256 + AES-256-GCM | `SingleCaseBundleEncryption`，只由 `SingleCaseBundleService` 调用 | 不整包入内存；预览和提交分别输入密码并认证 |
| 完整备份导出 | 明文风险确认后写出 ZIP，或把 ZIP 流式写入独立 v1 密码容器 `.nfbak` | `MainActivity` SAF → `StageTwoViewModel` → `CaseBackupService.export` | 真实系统 ZIP/密码容器、manifest/文件哈希与导出计数 |
| 完整备份只读预览 | 系统打开文档；密码文件先认证解密，再校验 ZIP，并逐命例对照当前库稳定 ID/出生输入/四柱 | `MainActivity` SAF → `StageTwoViewModel` → `CaseBackupService.preview` | 普通/密码文件显示候选原因、本地 revision 与回收站状态；非空库零写入 |
| 完整备份密码容器 | 独立保护版本 1；PBKDF2-HMAC-SHA256 600,000 次、AES-256-GCM 与参数 AAD；不整包入内存 | `BackupEncryption`，只由 `CaseBackupService` 调用 | 参数边界、认证失败、明文不可见及 Android `.nfbak` 往返 |
| 完整备份恢复计划 | Android 逐例选择按原 ID/跳过/保留两份/范围合并；全部覆盖后重新读取当前冲突并绑定 manifest | `FullBackupPreviewDialog` → `StageTwoViewModel` → `CaseBackupService.prepareRestorePlan` | 决策完备性、目标/范围约束、回收站拒绝和 `PREVIEW_STALE`；计划通过仍零写入 |
| 完整备份恢复执行 | 明文/密码备份最终确认后重读同一文件；按原 ID、保留两份、跳过或范围合并 | `MainActivity` SAF → `StageTwoViewModel` → `CaseBackupService.executeRestorePlan` | 文件与完整来源聚合一致、提交前冲突复查、子项 ID 重建、Room 原子事务 |
| 带附件范围合并 | 只收集所选记录/事件中实际新增内容引用的来源附件；跨模块按来源附件 ID 去重 | `SingleCaseExchangeService` 分析/重映射 → `CaseBackupService` 文件事务 | 新附件 ID/路径、引用一致；失败只清理本事务文件，目标既有附件和字段证据不变 |
| 恢复附件事务 | 导入命例的附件复制到事务专属暂存目录，校验后原子切换；不覆盖现有路径 | `CaseBackupService` → App 私有附件根目录 | 数据库失败只移除本次目录；现有命例和附件保持不变 |
| 中断恢复日志 | 启动及下次执行前扫描事务日志；无 DB 事实则回收附件，事实与附件完全一致则收尾 | `StageTwoViewModel` → `CaseBackupService.recoverInterruptedRestores` | 部分写入、载荷变化或附件不一致时停止自动处理并保留现场 |
| 进程强杀设备证据 | instrumentation 持久化真实私有边界并等待；主机强杀后以新 PID 冷启动验证 | `scripts/run_restore_process_recovery_e2e.sh` → `RestoreProcessRecoveryDeviceTest` | 脚本拒绝非模拟器；强杀后状态仍在，冷启动后日志/暂存/最终孤儿目录均消失 |
| 完整来源聚合 | 从备份 Room 实体、交叉引用与 sortOrder 重建完整领域命例，供冲突、差异和提交共用 | `RoomDataSnapshot.toDomainCases` → `BackupCaseRestorePreview.sourceCase` | 与原仓储聚合完全相等并执行 `BaziCase` 引用约束 |
| 共享合并分析 | 完整备份目标分析调用单命例同一内容去重与逐字段差异实现 | `CaseBackupService.prepareCaseMerge` → `SingleCaseExchangeService.analyzeMerge` | 候选复查、字段键和可追加数量一致；零写入 |
| 完整备份范围编辑 | 对具体活动候选显示模块数量和逐字段前后值；空范围不能确认，返回后保留其他逐例决策 | `FullBackupMergeDialog` → `StageTwoViewModel` | 模拟器从真实 ZIP 冲突候选进入、空范围禁用并返回预览 |
| 完整恢复批量工作台 | 全屏 `LazyColumn` 分离备份概览、逐例卡片与固定阶段动作；显示已决策数量 | `FullBackupRestoreWorkspace` → `StageTwoViewModel` | 大量案例只惰性组合；显式批量跳过不直接提交，计划与最终确认仍分离 |
| 本地冲突候选 | 稳定 ID、出生输入、四柱和回收站位置 | `CaseRepository` 查询，由预览服务合并 | 同一候选理由合并且不修改本地命例 |
| 无附件命例提交 | 跳过零写入；保留两份重建全部聚合 ID，提交前复查冲突 | `StageTwoViewModel` → `SingleCaseExchangeService.commitImport` → `CaseRepository.save` | 过期预览拒绝、事务失败不覆盖、系统文件 E2E 后列表读回第二份 |
| 范围化合并 | 模块仅追加独有内容；标量差异逐字段采用，默认本地 | `prepareMerge` 固定目标 revision/载荷 → `commitMerge` 复查 → `CaseRepository.save` | 内容去重、子项重建 ID、目标变化拒绝、模拟器记录/事件合并 |
| 附件二进制 | JSON 仍只保留引用；`.nfbcase` 才携带经 manifest 绑定的附件字节 | `REFERENCES_ONLY` / `BUNDLED_BINARIES` | JSON 引用拒绝提交；明文/加密包系统文件保留两份后字节与引用一致 |
