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
| 四柱反查 | `FourPillarsLookup` → `TymeFourPillarsLookup` | `StageTwoViewModel` | UI 直接调用 Tyme4j、把候选当出生分钟唯一证明或保存为正式命例 |
| 命盘图片导出 | `CaseImageExportContract` → `CaseImageRenderer` | 系统文件保存、长图分享 | 截取 Compose 可见视口、两套导出拼接、重新排盘或采用问真来源值 |
| 外部分析手动桥接 | `ExternalAnalysisBridge` → `ExternalAnalysisBridgeContract` | `StageTwoViewModel`、系统剪贴板、`TextRecordUseCase` | 页面重新投影字段、自动联网发送、无确认复制、无来源回填或把外部内容当算法真值 |
| AI 命盘分析指令 | `CaseObjectiveSummaryContract` → `BaziAiAnalysisPromptContract` | 基本排盘弹窗、系统剪贴板 | UI 自行拼命盘字段、自动联网、补造缺失资料或把模型推演标成输入事实 |
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
| 图片导入会话 | `ImportSessionRepository` | 图片入口、后台识别协调器、唯一 WorkManager 任务 | OCR、页面或 Worker 绕过仓储直接写 Room |
| 私有导入图片 | `PrivateImportImageStore` | Photo Picker、系统分享入口 | 后台任务长期持有外部 URI 或传递 Bitmap |
| OCR、长图与重复提示 | `core:image-parser` | `ImportRecognitionCoordinator` | 在线引擎进入主链、整张展开超大图、按相似哈希自动合并、单关键词猜测页面或直接写正式命例 |
| 用户列表精识别与日期一致性 | `OcrDocumentRefiner` → `WenzhenUserListOcrRefiner` → parser v9 → `WenzhenParseResultRefiner` → `FourPillarsLookup` | 识别协调器注入；复核 UI 只展示结果 | UI/Tyme4j 直连、来源星号补值、跨 OCR 块拼柱、跨列借字、日期冲突静默通过 |
| 通用脱敏诊断包 | `app/AppDiagnostics` | 设置页剪贴板入口 | 复制原始异常、命例身份、出生资料、OCR 内容、文件路径、URI、密码或附件事实 |

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
| 完整备份只读预览 | 系统打开文档；密码文件先认证解密，再校验 ZIP、写入独立临时 Room 数据库完整读回，并逐命例对照当前库稳定 ID/出生输入/四柱 | `MainActivity` SAF → `StageTwoViewModel` → `CaseBackupService.preview` | 临时库约束/实体/领域聚合往返通过；普通/密码文件显示候选原因、本地 revision 与回收站状态；非空库零写入 |
| 完整备份密码容器 | 独立保护版本 1；PBKDF2-HMAC-SHA256 600,000 次、AES-256-GCM 与参数 AAD；不整包入内存 | `BackupEncryption`，只由 `CaseBackupService` 调用 | 参数边界、认证失败、明文不可见及 Android `.nfbak` 往返 |
| 完整备份恢复计划 | Android 逐例选择按原 ID/跳过/保留两份/范围合并；全部覆盖后重新读取当前冲突并绑定 manifest | `FullBackupPreviewDialog` → `StageTwoViewModel` → `CaseBackupService.prepareRestorePlan` | 决策完备性、目标/范围约束、回收站拒绝和 `PREVIEW_STALE`；计划通过仍零写入 |
| 完整备份恢复执行 | 明文/密码备份最终确认后重读同一文件并再次执行独立临时库预演；按原 ID、保留两份、跳过或范围合并 | `MainActivity` SAF → `StageTwoViewModel` → `CaseBackupService.executeRestorePlan` | 文件与完整来源聚合一致、临时库再次通过、提交前冲突复查、子项 ID 重建、Room 原子事务 |
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

## Stage 6 信息架构与可恢复状态入口矩阵

| 入口/消费者 | 当前状态 | 唯一入口 | 最小验证 |
|---|---|---|---|
| 排盘首页最近命例 | 只读取活动命例中有 `lastViewedAt` 的最近 3 条，不另存显示副本 | `CaseRepository.search(LAST_VIEWED_DESC)` → `StageTwoViewModel.recentCases` | 仓储请求契约与 API 35 创建—查看—快捷返回流程 |
| 命盘详情四标签 | 基本信息、基本排盘、岁运、分析记录消费同一 `BaziCase` 与已采用快照 | `StageTwoUiState.detailSection` | 四标签真实切换；编辑、记录和岁运长流程回归 |
| 页面与草稿恢复 | 页面、参数、筛选、四类表单和详情标签写入 `SavedStateHandle`；密码、文件流和一次性句柄不保存 | `StageTwoViewModel` 保存状态合同 | Android 新 ViewModel 重建、后台 Activity 销毁、宿主杀旧 PID 后新进程恢复 |
| 页面无障碍与目标视口 | 主入口、命例表单、详情四标签、分类、记录和事件页为可操作节点提供名称、点击语义和至少 48dp 目标 | Compose 语义树与同一真实工作流 | 手机/展开态 × 1.0/2.0 字体四组合，其中 TalkBack 开/关各两组；脚本恢复系统设置 |

## Stage 7A–7C 岁运定位合同

| 能力 | 唯一入口 | 事实边界 |
|---|---|---|
| 流年序列 | `BaziEngine.calculate()` → `CalculationResult.annualFortunes` | 从标准公历出生年生成至 120 年／十二步大运终点；保存干支、年份、虚岁和摘要大运归属 |
| 当前流年 | `ProfessionalFortuneResolver.locate()` | 观察日期与时分由用户配置，默认当地中午；按配置中的精确立春切换，不按公历元旦切换 |
| 当前大运 | `FortunePositionResolver.locate()` | 优先使用每步大运的 `[startAt, endAtExclusive)` 精确半开区间；旧快照无精确边界时才按年份降级 |
| 起运前小运 | `ProfessionalFortuneResolver.locate()` | 从已采用时柱下一位按大运既有顺逆逐年生成，只覆盖出生至精确交运前的半开区间；返回年份、周岁、干支、十神与藏干。UI 仅合并为大运行最左一列，点击后复用流年行，不推算也不单独占行 |
| 流月/流日/流时 | `ProfessionalFortuneResolver.locate()/select()` | `locate()` 按自由观察时刻定位全部层级；`select()` 消费带层级的选择命令并校验父层不变。流月只在十二节切换；流日按当前节令月与父区间交集生成；流时服从快照子时规则并保持流日父级；观察时刻按民用时直接计算 |
| 子时口径 | `LunarHour.resolveEightChar()` | 正向计算按调用显式选择实例 provider，不读取或改写全局状态；仅四柱反查按 D-077 在适配器锁内临时切换并恢复 `LunarHour.provider` |
| 页面消费 | `StageTwoViewModel` → 专业细盘 | 五层点击由 UI 传 `ProfessionalFortuneSelection(layer, observedAt)`，ViewModel 调用领域 `select()` 并在成功后原子替换观察时间和结果；校验失败保留原结果。自由日期与今天继续走 `locate()`。九列与时间轴只消费领域输出，UI 不计算年龄、藏干、月界或父层归属 |
| 专业细盘关系与基础神煞 | `ProfessionalFortuneResolver` → `professional-detail-relations-shensha-v4` | 以已采用原局上下文和九列干支统一输出去重后的干支关系与版本化基础神煞；神煞只显示实际命中条目，按稳定优先级每柱最多展示 5 项，并以实际干支为行首逐时间柱输出；UI 不重算、不回写问真截图来源对照、不生成吉凶断语 |
| 问真基本资料衍生字段 | `BaziEngine.calculate()` → `BasicChartDetails` | 前后“节”与相邻二十四节气分别保存；胎元、胎息、命宫、身宫及前后节只在正式提交后对照，不由 OCR 决定算法真值 |
| 问真用户列表正式提交 | `ScreenshotImportCommitter` → `FourPillarsLookup` → `BaziEngine.calculate()` → `CaseRepository` | 只按同一公历日期保留两种子时口径下可复算的民用候选；全部标记 `DOUBLE_HOUR_ONLY`，无解显式拒绝，不按时支硬填整点、不推算真太阳时 |
| 计算档案升级差异 | `CaseCalculationSnapshot` → `compareCalculationSnapshots()` → 基本排盘页 | 当前采用快照只与最近历史快照比较；输入或规则配置变化优先阻断版本归因，输入和口径一致时才把引擎/规则版本变化标为可核对升级 |
| 问真无算法字段 | `WenzhenSourceFidelityContract` → parser v8 → 字段证据/核对页 | 星宿、命卦、五行与党派比例、自定旺衰/格局和四柱神煞只保留原文、规范值、修正、置信度、原图框；提交后仍禁止 calculatedValue/一致性 |
| 命例客观对比 | `CaseRepository` → `CaseComparisonEngine` → `CaseComparisonScreen` | 只读取两个活动命例及各自已采用快照，分出生历法、基础命盘、岁运、计算档案和研究资料显示相同/不同/缺失；禁止生成吉凶、合婚或关系结论 |
| 四柱反查 | 首页地区／时间 → `FourPillarsLookup.search()` → `TymeFourPillarsLookup` → `BaziEngine.calculate()` 复核 → 用户点选回填 `CaseFormState` | 首页地区是时区的唯一来源；四柱面板只编辑四柱与年份范围，不重复显示或修改地区／时区。只查 1800–2200 的民用时，DST 重叠按 offset 分列、不存在时刻排除；`getSolarTimes` 所需全局 provider 只在适配器锁内临时切换并恢复；原始反查为空时仅在适配器内按 60 日周期扫描民用代表时刻且仍由唯一正向引擎复算；点选只回填日期、时辰和 offset 并标记 `DOUBLE_HOUR_ONLY`，不改写地区／时区，不伪造真太阳时/精确分钟，候选不自动保存 |
| 北京时间默认 | `BaziTimeZoneDefaults` → 新建/恢复/导入/国内地点/备份命名 | `Asia/Shanghai` 是唯一默认值与备份文件名时区；只在缺失值时回退，历史命例与用户选择的 IANA 时区保持原样 |
| 命盘图片导出与分享 | `CaseImageExportContract.prepare()` → `AndroidCaseImageRenderer` → SAF/FileProvider | 领域合同只投影唯一已采用快照和正式记录；保存与分享缓存并复制同一 PNG 字节，系统取消、输出失败、无分享目标和分享启动失败返回稳定错误码，页面不离开当前详情 |
| 客观命盘摘要 | `CaseObjectiveSummaryGenerator` → `CaseObjectiveSummaryContract` → 摘要页/剪贴板/图片合同 | 只投影唯一已采用快照和正式资料计数；固定字段来源与缺失状态，页面和图片不得重算或生成主观解释 |
| 外部分析手动桥接 | `CaseObjectiveSummaryGenerator` → `ExternalAnalysisBridge.prepareExport/prepareImport()` → 剪贴板/`TextRecordUseCase` | 只消费同一客观摘要投影；按字段组预览，默认隐藏身份、精确出生时间、地点和时区，复制与回填分别主动确认；草稿可恢复但确认不跨重建，旧命例 revision/快照拒绝回填；结果仅保存为带来源的 `ANALYSIS`，无网络客户端 |
| 师傅点评观点候选 | `MasterCommentaryCandidateExtractor` → `DeterministicMasterCommentaryCandidateExtractor` → `TextRecordUseCase.adoptCommentaryCandidate()` | 完整点评原文和历史仍是唯一来源；解析层只产出稳定区间、分类建议与规则证据，UI 不含规则；编辑/拒绝为审核状态，采用只新增正式分析并校验来源 revision、区间与聚合 revision |
| 命主反馈主题候选 | `FeedbackThemeCandidateExtractor` → `DeterministicFeedbackThemeCandidateExtractor` → `CaseMetadataUseCase.adoptFeedbackThemeCandidate()` | 完整反馈、历史和既有事件仍是来源事实；解析层按稳定区间聚合规范主题，UI 不含规则；编辑/拒绝为审核状态，采用只追加正式标签并校验来源 revision、证据与聚合 revision |

## alpha72 问真式界面与来源治理

| 概念 | 唯一所有者/入口 | 边界 |
|---|---|---|
| 三主界面导航 | `StageTwoNavigator` + `StageTwoViewModel` | 根级只保留排盘、记录、设置；导入、对比、备份等能力仍复用原有用例，只改变入口层级，不复制业务逻辑 |
| 问真式视觉壳 | `NanfengBaziTheme` + Compose screen components | 参考信息层级、密度和交互位置，使用南枫本地绿/黑金皮肤；不复制第三方素材或形成页面算法 |
| 记录密集摘要 | `CaseRepository.search()` → `CaseSummaryRow` | 只消费命例聚合和已采用快照；四柱着色、生肖和 A–Z 索引只是展示，不重算、不改写事实 |
| 详情四标签 | `CaseDetailScreen` + `StageTwoUiState.detailSection` | 四页共用同一命例与已采用快照；基本信息使用居中身份区，基本排盘与专业细盘共用公历／农历／乾坤造摘要头，断事笔记独立显示乾坤造、四柱和前十步大运；低频管理动作统一进入右上角菜单 |
| 记录来源类型 | `CaseTextRecord.sourceType` + `TextRecordSourceType` | Room v8 持久化 USER/RULE_TEMPLATE/EXTERNAL_AI/IMPORTED_IMAGE/历史未指定；历史缺省只按是否有来源附件归一，不根据文本猜测来源 |
| App 图标主图 | 用户提供的原始附件 | 构建、分层、缩放与真机门禁以 [启动图标保真构建规范](app-icon-fidelity-standard.md) 为唯一正文；禁止近似重绘或额外托盘 |

## alpha73 低输入选择器与图标资源边界

| 概念 | 唯一所有者/入口 | 边界 |
|---|---|---|
| 日期时间滚轮 | `BirthDateTimePickerSheet` → `CaseFormState` | 固定底部弹窗在 API 35 目标画幅至少 560dp；滚轮独占纵向手势，弹窗不可随滚轮拖动。只选择公历/农历民用输入，合法日期、闰月、时区与排盘仍由领域/引擎验证 |
| 地点三级联动 | `BirthplaceCatalog` → `BirthplacePickerSheet` → `CaseFormState` | 与出生时间共用 680dp 固定底部弹窗、外部空白点击关闭和滚轮独占纵向手势。离线目录提供地点名、IANA 时区及仅在可信时存在的城市中心参考坐标；缺少坐标不得用省会／猜值填充，真太阳时默认关闭 |
| 四柱查询选择器 | `FourPillarsWheelPickerSheet` / `YearRangeWheelPickerSheet` / `IanaTimeZoneWheelPickerSheet` → `FourPillarsLookupFormState` | 四柱页以单一编辑焦点联动柱位、对应天干／地支圆槽及下方选区；天干选定后焦点进入同柱地支，也允许直接点击圆槽切换编辑位。焦点主强调只属于八字圆槽，下方选区使用不改变几何的弱状态；表示层只构造有效六十甲子和公开查询边界，查询、结构化错误及候选复算仍由 `FourPillarsLookup` |
| 离散触觉反馈 | Compose `ValueWheel` → Android `performHapticFeedback(CLOCK_TICK)` | 只在中心刻度变化时反馈并尊重系统触觉开关；不申请振动权限，不用触觉表示计算正确 |
| 启动图标 | `design/assets/app-icon-master.jpg` → `app_icon_source`、`mipmap-*` / adaptive icon | 用户提供 JPEG 是唯一当前母版；两层 adaptive 构建与启动器核对严格按 [启动图标保真构建规范](app-icon-fidelity-standard.md) 执行 |

## alpha74 详情视觉、星座资源与文案边界

| 概念 | 唯一所有者/入口 | 边界 |
|---|---|---|
| 西方星座真值 | `CalculationResult.basicChartDetails.westernZodiac` → `CaseSummary.westernZodiac` | 由唯一排盘引擎产出并随采用快照进入列表；UI 不按公历日期重新判断星座 |
| 星座表现资源 | `CaseSummaryRow` / 详情身份头 → `zodiac_*.xml` | 只把引擎中文星座名映射到 Tabler Icons 的 12 个 MIT 矢量资源；图形和“处女座”等名称同处黑金圆标，不改变算法真值 |
| 子时默认口径 | `CalculationPreferenceStore` → `StageTwoViewModel` → 新表单/反查表单 | 设置页持久化默认值，只影响新查询和新计算；既有计算快照不被静默重算 |
| 首页与详情视觉 | `NanfengBaziTheme` + Compose 共享组件 | 问真负责信息架构与密度参照，南枫记提供字体、圆角、颜色、滚轮和触觉；UI 仍只消费公开领域接口 |
| 辅助文案 | 各页面 Compose 文案 → 视觉 QA | 复述“点击/滑动/三级联动”或重复相邻值的小字不显示；只有业务口径、风险、证据边界、异常和写入后果可占辅助说明层 |
| 本地预览案例 | `RecordPreviewFixtureTest` + 显式 `seedPreviewCases=true` | 仅在 API 35 视觉验收时写入姓名化合成案例；正常测试会跳过，正式构建与首次启动不内置、不自动生成 |

## alpha75 本地万年历边界

| 概念 | 唯一所有者/入口 | 边界 |
|---|---|---|
| 万年历领域合同 | `AlmanacReader`、`AlmanacMonthQuery`、`AlmanacResult` | `core:domain` 定义查询、42 格月份、日详情和结构化错误，不依赖 Tyme4j 或 Compose |
| 万年历生产实现 | `TymeAlmanacReader` | 只在 `core:engine-tyme` 调用 Tyme4j；UI 不读取远端接口、不嵌 WebView、不复制第二历法算法 |
| 首页与日期回填 | `StageTwoViewModel` → `AlmanacScreen` → 正常排盘表单 | 月份、所选日、所选时辰和目的地进入 `SavedStateHandle`；“用此日期排盘”回填公历日期与时辰代表时刻，完整计算仍经过 `BaziEngine` |
| 响应式表示 | `AlmanacScreen` | 所有宽度保持“月历在上、日详情在下”的同向信息流；760dp 以上仅加大整宽月历格与卡片间距，不将日详情挤入窄右栏。宽度只改变布局，不改变领域结果或缓存第二份事实 |
| 时辰与八字详情 | `AlmanacMonthQuery` → `TymeAlmanacReader` → `AlmanacDayDetails` | 12 时辰、四柱、十神、藏干十神、年月日时基础神煞和天干五合／地支六合六冲由同一引擎结果及共享 `BasicShenShaRules` 产出；Compose 只呈现，不维护第二套干支、十神或神煞规则 |
| 万年历稳定刷新 | `StageTwoViewModel.loadAlmanac()` → 单一待处理查询 → `AlmanacMonthView` 原子替换 | 日期、时辰、月份与“今天”刷新期间保留完整旧画面；成功后一次替换查询坐标和详情，失败保留原画面。节气／节日固定在农历日期同行；干支关系保留为领域结果但不在详情页另设重复说明卡 |
| 五行视觉语义 | `BaziElementPresentation` | 记录列表、四柱录入、万年历和基础排盘都调用同一字符／元素映射；庚辛申酉固定亮黄色，颜色不参与领域计算 |
| 传统资料说明 | `AlmanacDayDetails` → 详情页 | 宜忌与称骨作为传统民俗资料展示；称骨以独立版本化权重表和男女断语表呈现，明确标注民俗、版本差异与非事实边界，绝不自动写入命例；建除、值神、星宿、胎神不进合同或 UI |

## alpha76 UI-11 展开态边界

| 概念 | 唯一所有者/入口 | 边界 |
|---|---|---|
| 截图审阅双栏 | `ScreenshotImportReviewScreen` → `ScreenshotEvidencePreview` | 左栏只读取当前 `ScreenshotImportUiState` 中已私有复制的原图和同一字段来源框；右栏继续采用、确认或保留候选，不重跑 OCR、不补全字段。 |
| 岁运双栏 | `CaseDetailContent` → `FortuneDetailsView` | 概览、大运和流年详情共同消费同一已采用 `CalculationResult` 与当前定位结果；布局不得自行复算四柱、岁运或生成第二快照。 |
| 展开断点 | `StageTwoScreens.kt` | 只有全屏 `>=840dp` 且嵌套详情内容至少 `360dp` 时并列；不足时保持单列。断点只改变表示，不改变保存、候选或领域状态。 |

## alpha82 命例详情与区县目录边界

| 概念 | 唯一所有者/入口 | 边界 |
|---|---|---|
| 命例详情投影 | `CaseDetailScreen` → 当前命例/已采用 `CalculationResult` | 居中基本身份区、紧凑详情身份条、连续信息表、四柱矩阵、九列流运总览、120 年大运／十年流年和反馈时间线只是同一模型的表示；职业等主观资料读取 `CaseProfile`，不重算、不回写、不新建详情专用真值 |
| 五行呈现 | `BaziElementPresentation` | 天干、地支、藏干、流运及候选都调用同一字符到色彩映射；金的亮黄色与记录页同源，底板不承担真值或状态含义 |
| 信息表底色 | `WenzhenFactRow` / `WenzhenDualFactRow` / `BasicChartDetailsView` | 一般事实行严格灰白交错；基本排盘的天干、地支属于连续八字本体，固定同为白底，交错规则让位，下一行藏干承接灰底后继续白灰交替 |
| 管理动作 | 详情右上角菜单 → 既有用例 | 编辑、导出、复制、软删除等只调整入口优先级；不删除功能、不跳过确认、不改变用例边界 |
| 国内行政区目录 | `china-districts.csv` → `BirthplaceCatalog` | 锁定上游提交生成 2,846 条省／市／区县名称；仅补全选择目录和北京民用时，不用区县名伪造经纬度或真太阳时证据 |
