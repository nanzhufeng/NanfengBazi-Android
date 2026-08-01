# 当前交接：alpha73 低输入滚轮、问真式视觉与保真图标

更新日期：2026-08-02

## A. 现场快照

- 项目：南枫八字，本地优先 Android App。
- 仓库：`/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi`
- 分支：`main`；本地准确提交以现场 `git log -1` 为准，禁止 push/发布。
- 版本：`versionCode 74`，`versionName 0.3.0-alpha73`。
- 本轮只向 `emulator-5554` 下发 ADB 命令，现场确认 API 35；未操作 OPPO。
- 本轮未联网、未调用外部 AI、未写入真实个人资料，模拟器案例均为合成测试数据。
- 现场代码和最新测试证据优先于本文；若不一致，先修正本文。

## B. alpha73 已完成

### 低输入选择与三主界面

- 排盘首页继续按用户提供的问真参考：顶部“首页排盘”、白色圆角表单卡、
  性别与公历/农历/四柱分段、时间与地区、保存开关、黑金“开始排盘”、即时排盘卡和能力入口；
  Material 图标替换文字假图标，比例、间距和信息密度已在 API 35 重新校准。
- 新建、编辑和候选表单的公历/农历日期时间统一为五列联动滚轮；默认预填
  `1990-01-01 00:00` 并自动生成可编辑别名。未知地点保持未选，不静默猜北京。
- 出生地点统一为国内/海外与省/市/区县联动滚轮；确认后写入 IANA 时区和明确标注的
  城市中心参考坐标。四柱反查的四柱、年份范围和 IANA 时区也改为滚轮选择。
- 记录页直接展示全部保存案例：用户列表/回收站、搜索与筛选、密集案例行、元素着色四柱、
  黑金生肖圆标与 A–Z 索引。新排盘、截图建档、单例导入、对比、备份/恢复保留在更多菜单。
- 设置页改为白底分组列表，继续接通本地导入、完整备份、恢复和诊断版本能力。
- 底部导航收口为带真实 Material 图标的白色三入口“排盘/记录/设置”，不复制未接通的学堂、
  聊天、VIP 或 AI 入口。

### App 图标

- 用户提供的原图已保存为 `design/assets/app-icon-master.png`，并生成 legacy 和 adaptive
  各密度启动资源；主体未裁切、未拉伸、未近似重绘。
- 原图仅因 1448×1444 的近方形尺寸补齐 4px 原背景色边缘，API 35 启动器遮罩内主体完整。

### 案例详情

- 顶部为“南枫八字”与四标签：基本信息、基本排盘、专业细盘、断事笔记。
- 黑金身份头在四标签切换后固定保留；基本排盘继续显示四柱、主星、藏干、副星、星运等已有表格。
- 低频命例管理动作默认折叠，编辑资料和管理分类保留在首屏；自动测试已按显式展开语义更新。

### 数据和证据

- Room 升级至 v8，`CaseTextRecord` 新增持久化 `TextRecordSourceType`；历史有来源附件者
  归一为 `IMPORTED_IMAGE`，其余归一为 `USER`。备份、单例交换、历史和外部分析回填保留来源。
- 分析分类增加“关键年份”和“待核对问题”。
- `four-pillars-golden-v2.psv` 升级为 schema v2：60 条样本显式保存性别与证据元数据，
  并明确它们是合成/冻结回归证据，不是真实问真案例。
- 线上算法入口未改：UI 不调 Tyme4j，四柱反查仍经 `FourPillarsLookup` 与唯一正向引擎复核。

## C. 最新验证

- 最终组合 `test lint assembleDebug assembleRelease assembleDebugAndroidTest --offline`：
  `BUILD SUCCESSFUL in 2m 26s`，399 个 Gradle 任务。
- `emulator-5554` API 35：`AutomatedPickerFlowTest` 串行 `OK (2 tests)`，覆盖日期/地点预填滚轮
  与四柱/年份范围/IANA 时区滚轮；全仓 JVM 已继续覆盖 VX-09 无解、非法干支、范围边界、
  60 年周期、两种子时口径和全局 `LunarHour.provider` 状态恢复。
- 人工视觉复验已覆盖最终首页、日期时间弹层、地点弹层和启动器图标；参考/实现并排证据见
  `design/qa/alpha73/reference-comparison.jpg`，完整说明见 `design-qa.md`。

alpha73 产物：

- `app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha73-debug.apk`
- `app/build/outputs/apk/release/NanfengBazi-Android-v0.3.0-alpha73-release-unsigned.apk`
- `app/build/outputs/apk/androidTest/debug/NanfengBazi-Android-v0.3.0-alpha73-debug-androidTest.apk`

## D. 明确待办与禁止项

### 仍需外部条件

- 真实问真基本资料/专业细盘/点评页迁移准确率：需继续授权样本和逐字段确认。
- OPPO Find N5 安装、折叠/展开、数据保留与朗读：需用户明确授权，本轮未触碰。
- 正式签名、发布、GitHub Release 和回滚：需签名材料与发布授权。
- 禁止：不联网、不接外部 AI、不接触 OPPO、不 push、不发布、不用模拟器或合成数据冒充真机/真实资料证据。

## E. 下一轮直接启动

1. 先只读确认 Git 根、分支和工作区计数，然后读 `AGENTS.md` 与本文。
2. 没有新外部资料或授权时，只继续可在本地证明的功能，不要要求用户再发“继续”交接语。
3. 真机授权到位后按顺序验收 OPPO 安装、折叠/展开、数据保留、朗读和真实问真整例；
   在此之前不得把模拟器、合成样本或视觉相似度冒充真机/真实资料证据。

---

# 历史交接：alpha70 VX-11 无网络外部分析手动桥接

更新日期：2026-08-01

## 1. 接手快照

- 项目：南枫八字，本地优先 Android App。
- 仓库：`/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi`
- 当前分支：`main`
- alpha70 接手基线：`a4e76b0 docs: align VX-09 year contract`；本轮在其上实现 VX-11
  纯本地手动桥接，最终本地 checkpoint 以现场 `git log -1` 为准。
- 版本：`versionCode 71`，`versionName 0.3.0-alpha70`
- 本轮开始前工作区干净；接手时仍须现场复查当前提交和工作区。
- alpha70 验收只向 `emulator-5554`（API 35）下发命令；未操作 OPPO。
- 本轮使用两张用户授权真实问真列表做脱敏指标验收；原图、OCR 原文和姓名未进入 Git
  或正式测试输出。生日与四柱只在不含姓名的一次性模拟器诊断中用于逐行核对；诊断源码、
  文本和模拟器副本均已删除，未进入 Git 或正式产物。
- 本轮未操作 OPPO、网络、外部 AI、远端推送或发布；VX-11 只使用系统剪贴板和本地 Room。

现场事实优先级：当前代码与最新测试证据 > 本文件 > 稳定项目文档 > 历史聊天。

## 2. 产品目标与首要验收

南枫八字用于本地管理命例、版本化排盘、研究记录和问真截图迁移。

“问真输出是首要验收标准”的准确含义是：

1. 用户会把问真八字中已经保存的案例，通过用户列表、基本资料、基本排盘、专业细盘、
   命主反馈和师傅点评等截图导入南枫八字。
2. 当前首先适配问真八字截图格式；不承诺兼容所有排盘软件。
3. 截图来源值、OCR 规范值、人工采用值和南枫本机计算值必须分离保存。
4. 截图迁移保真不等于算法真值；正式排盘仍只能经过版本化计算入口。
5. 自动化不得生成确定性吉凶、合婚、事业、婚姻或健康结论。

## 3. 接手前必须读取

按顺序读取，避免加载不必要历史：

1. `AGENTS.md`：每轮必须遵守的安全边界和入口。
2. 本文件：当前代码事实、证据、未完成和下一任务。
3. `docs/REQUIREMENT_GAP_AUDIT.md`：全方案逐项状态。
4. `docs/architecture-governance.md`：概念所有者、公开入口和模块边界。
5. `docs/domain-rules.md`：本项目稳定业务规则。
6. 只在需要时读取 `docs/PRODUCT_REQUIREMENTS.md`、`docs/TEST_STRATEGY.md`、
   `docs/DATA_CONTRACT.md` 和 `docs/decision-log.md` 的相关段落。

不要把旧聊天摘要当当前事实，不要一次性读取完整历史日志。

## 4. 当前架构与唯一所有者

| 概念 | 唯一所有者/入口 | 禁止分叉 |
|---|---|---|
| 排盘计算 | `BaziEngine.calculate()` → `TymeBaziEngine` | UI、OCR、数据库直接调用历法库 |
| 四柱反查 | `FourPillarsLookup` → `TymeFourPillarsLookup` → `BaziEngine.calculate()` 复核 | UI 直接调用 Tyme4j、展示未经复核候选或把候选自动保存为命例 |
| 命盘图片导出 | `CaseImageExportContract` → `CaseImageRenderer` | 截取 Compose 可见视口、保存/分享各拼一套内容或重新排盘 |
| 客观命盘摘要 | `CaseObjectiveSummaryGenerator` → `CaseObjectiveSummaryContract` | UI、剪贴板或图片各自拼字段、重算旧快照或生成主观解释 |
| 计算配置 | `CalculationProfile` | 页面用默认值重解释旧快照 |
| 命例聚合写入 | `CaseRepository` | 页面或导入器直接写 DAO |
| 完整备份恢复 | `CaseBackupService` | UI 自行解析、合并或恢复 |
| 单命例交换 | 对应 exchange/bundle service | 将引用元数据冒充附件字节 |
| 问真来源字段 | `WenzhenSourceFidelityContract` | 来源值覆盖本机真值 |
| 图片导入会话 | `ImportSessionRepository` + recognition coordinator | 后台任务直接生成正式命例 |
| 命例客观对比 | `CaseComparisonEngine` | 生成吉凶、合婚或关系结论 |
| 页面恢复状态 | `StageTwoViewModel` + `SavedStateHandle` | Activity 与局部页面各存一套状态 |

物理模块：

- `core:domain`：模型、接口、领域合同。
- `core:engine-tyme`：Tyme4j 1.5.1 唯一生产适配器。
- `core:solar-time`：真太阳时计算及证据。
- `core:data`：Room、仓储、交换、备份与附件事务。
- `core:image-parser`：离线 OCR、问真分类、解析和图片指纹。
- `app`：用例协调、Compose UI、系统文件/分享/后台任务适配。

## 5. 已完成到哪里

### v1.0 核心

- 公历、农历、闰月、地区、经纬度、IANA 时区、DST 重叠选择和不存在时刻拒绝。
- 民用时/真太阳时、两种子时口径、标准公农历转换及完整版本证据。
- 四柱、生肖、星座、十神、藏干、纳音、长生、空亡、胎元、胎息、命宫、身宫。
- 起运方向/年龄/精确交运、前八步大运、完整流年、流月、流日和流时。
- 命例新建、即时排盘、编辑重算、多出生时间候选、搜索筛选、分类、记录、事件、
  历史版本、复制、软删除和回收站。
- 单命例 JSON、带附件 `.nfbcase`、完整 ZIP 备份、密码保护、冲突计划、事务回滚、
  独立临时 Room 数据库预演和真实进程强杀恢复。
- 问真 P0/P1/P2 截图导入基础设施、bundled ML Kit 离线 OCR、长图分段、多图归组、
  来源证据、人工修正、正式复算和可恢复导入会话。
- 手机/展开态自适应、四主入口、详情四标签、可恢复页面/草稿、字体放大和 TalkBack
  模拟器矩阵。

### alpha61

- 增加两个活动命例的客观对比入口。
- 只读取各自最新已采用快照和正式研究资料。
- 按出生历法、基础命盘、起运大运、计算档案、研究资料五层显示相同/不同/缺失。
- 左右命例稳定 ID 和页面状态可恢复。
- 明确不生成吉凶、合婚或关系判断。

### alpha62

- 增加“排盘 → 四柱反查”真实入口，输入四柱、1900–2100 年范围、IANA 时区和两种
  子时口径。
- `core:domain` 已定义查询、候选、结构化错误和 `FourPillarsLookup` 公开接口；
  `core:engine-tyme` 使用 `EightChar#getSolarTimes` 生成原始候选。
- 每个候选按 IANA 时区解析 UTC offset；DST 重叠按 offset 分列、不存在时刻排除，
  并再次经过 `BaziEngine.calculate()` 复算一致才返回。
- `LunarHour.provider` 只在反查适配器进程级锁内临时切换，成功、异常、取消和并发后
  均恢复；既有正向排盘继续使用实例 provider。
- 页面明确候选只是民用代表时刻，不是出生分钟唯一证明；首版不做真太阳时推算，
  不自动保存正式命例。
- 反查页面、表单、查询参数和已查询标记进入 `SavedStateHandle`；重建后重新查询，
  不保存可能过期的候选副本。

### alpha63

- 两张授权真实问真用户列表长图均由 bundled ML Kit 在 `emulator-5554` 离线识别，
  页面分类均为 `USER_LIST`，共保留 49 个独立日期行候选。
- 修复真实列表中姓名块漏识别后被下一日期重复复用的问题；解析器 v8 改用日期行中点
  划分区域，姓名/性别漏识别时仍保留可定位、可人工修正的证据，不再让空行拖垮整图。
- 49 条候选中姓名/性别自动规范化 43 条，四柱自动完整规范化 16 条；16 条完整四柱
  在两种子时口径下均能由 VX-09 找到同日民用候选并通过唯一正向引擎复算。
- 问真列表正式提交器已复用 `FourPillarsLookup`，保存所有去重后的同日民用时辰候选，
  只采用一个确定性代表候选；全部候选均标记 `DOUBLE_HOUR_ONLY`，不推算真太阳时。
- 核对页固定显示“候选不是出生分钟唯一证明”的说明；生日与四柱在两种口径下都无解时
  显式阻止写入，并提示核对 OCR、原图或问真口径。
- 一条真实候选完成私有复制、OCR、字段采用、反查、正向复算、附件复制、Room 写入，
  并在宿主进程强停后成功读取；验收后模拟器敏感数据已清除。

### alpha64

- `core:domain` 增加图片文档 v1、导出输入、结构化渲染事实、稳定失败码和
  `CaseImageRenderer` 公开端口；只允许唯一已采用快照和当前正式记录进入图片。
- Android 渲染器生成完整 1080px 宽 PNG，支持中文、长记录换行和 24,000px/像素上限；
  超限明确拒绝，不截取当前 Compose 视口。
- 详情页增加“导出图片”和“分享长图”，两者按命例 revision 与采用快照复用同一
  `RenderedCaseImage` 字节；命例事实变化后缓存自动失效。
- SAF 系统文件写入、受限 FileProvider 和系统分享选择器已接通；取消、输出失败、无
  分享目标及启动失败均返回结构化结果，并保留当前详情和已生成图片。
- 图片固定显示文档/命例修订、计算档案、来源边界和隐私提示；分享只声明交给目标应用，
  不误报为已经发送。

### alpha65

- `core:domain` 增加客观摘要 v1、固定五层章节、字段来源、缺失状态、稳定失败码和
  `CaseObjectiveSummaryGenerator` 公开入口；只允许唯一已采用快照进入摘要。
- 摘要覆盖出生资料、四柱与基础盘、起运大运、计算档案和正式资料计数；旧快照缺少
  转换、基础盘、前后节或精确边界时明确显示未记录，不按当前规则反推。
- 师傅点评、命主反馈、关键事件和附件只进入计数，不读取原文生成算法结论；模板标题、
  标签和说明有禁止性文案扫描。
- 详情页增加客观摘要入口，系统剪贴板不可用和异常分别返回结构化失败；复制成功由
  剪贴板读回验证，页面与命例稳定 ID 可经 Activity 及新 ViewModel 重建恢复。
- 图片导出改为复用同一客观摘要字段投影；修正图片曾把 `FortuneStart.startAt` 误标为
  精确交运的问题，现在与详情统一读取 `FortuneStart.endAt`。

### alpha66

- `core:domain` 增加点评候选 v1、UTF-16 半开原文区间、规则证据、审核状态、结构化
  提取/采用错误和 `MasterCommentaryCandidateExtractor` 公开入口。
- `core:image-parser` 增加本地确定性实现：只按中文/ASCII 标点与换行切分可定位句段，
  重复句保留首次出现，候选稳定 ID 绑定来源记录、revision、区间和原文；九类建议采用
  保守本地词表，UI 不含第二套规则。
- 点评候选页明确显示规则版本、来源 revision、原文区间、分类建议、规则证据及
  “候选不代表观点正确、不是本机算法结论”；支持编辑、拒绝、恢复和逐条采用。
- 编辑/拒绝只属于 `SavedStateHandle` 审核状态；采用通过既有 `TextRecordUseCase`
  重读聚合，核对来源类型、revision、区间原文和聚合 revision，只新增正式
  `ANALYSIS`，完整点评、历史与附件引用保持不变。
- 修复恢复竞态：候选 Bundle 先恢复、详情尚未重载时按钮禁用，调用层返回
  `CONTEXT_NOT_READY`，不再无声丢弃采用操作。

### alpha67

- `core:domain` 增加反馈主题候选 v1、UTF-16 半开证据区间、规范主题、分类建议、审核
  状态、结构化提取/采用错误和 `FeedbackThemeCandidateExtractor` 公开入口。
- `core:image-parser` 增加本地确定性实现：只按中文/ASCII 标点和换行切分可定位句段，
  用保守词表聚合学业、事业、财运、感情、家庭、健康与迁移主题；候选 ID 绑定来源、
  revision、规范主题和证据区间，UI 不含第二套规则。
- 主题候选页显示规则版本、来源 revision、规范主题、事件分类建议、逐段证据和规则说明，
  并明确“候选只是标签建议，不代表用户确认，也不是本机排盘算法真值”。
- 编辑、拒绝与恢复只属于 `SavedStateHandle` 审核状态；采用通过
  `CaseMetadataUseCase.adoptFeedbackThemeCandidate()` 重读聚合并校验来源类型、revision、
  全部证据区间和聚合 revision，只追加一个正式标签，优先复用同名全局标签 ID。
- 已有同名标签、标签上限、来源或证据过期、聚合冲突与存储失败均结构化拒绝且零写入；
  完整命主反馈、记录历史和既有事件保持不变。

### alpha68

- `OcrDocumentRefiner` 把页面特定精识别留在 `core:image-parser`；用户列表按日期行分别
  放大身份区、四柱区和四个字位，多阈值 bundled ML Kit 结果回映原图，UI 无 OCR 规则。
- parser v9 去重同一物理日期锚点，支持带括号时柱注记姓名、姓名/性别分块和逐列四柱；
  四柱必须属于六十甲子，来源 `*`/`＊` 明确保持未知，不从邻近像素或历法补值。
- `WenzhenPillarDateConsistencyRefiner` 通过 VX-09 公开接口分别检查两种子时口径的同日
  候选；匹配记一致性，冲突保留来源值并在复核页警告，正式提交仍由既有反查门禁阻断。
- 两张真实列表稳定拆出 20+29 行，姓名/性别 49/49；43 条合法完整 OCR 四柱中同日一致
  30、冲突 13，另有 6 条未完整，其中 1 条来源本身以星号隐去时柱。该分层指标取代
  alpha63 的 43 条身份/16 条四柱旧证据。该初始指标随后被 alpha69 的逐行原图复核
  纠正，不再作为当前基线。

### alpha69

- 新增用户列表灰色日期列二次扫描，只补首次 OCR 附近没有日期锚点的新块；第一张由
  20 条恢复为 21 条完整日期候选。已有日期不参与二次置信度竞争，避免 11 月被覆盖成
  1 月；截图底边另有 1 条缺完整生日的截断记录，保持非候选。
- 姓名字符合同补充 ASCII 句点，真实两图身份达到 50/50。日期列补锚、身份区和四柱
  复识别都只追加带原图坐标的证据，UI 无 OCR 规则。
- parser v9 禁止把多阈值产生的四个成对块跨块拼为四柱；优先单块四柱或完整天干/地支
  行，空间证据以天干列为基准要求地支同列一一配对。缺字即保持未知，不跨列借字。
- VX-09 年份合同扩为 1800–2100。真实样本暴露 Tyme4j 原始 `getSolarTimes` 对一条可由
  正向引擎在当天 05:00/06:00 复算的四柱返回空；适配器在原始为空时按日柱 60 日周期
  扫描民用代表时刻，每个候选仍经唯一 `BaziEngine.calculate()`、IANA 时区、DST 和
  子时口径复核。全范围无解诊断约 843ms，页面既有 `ioDispatcher` 隔离主线程。
- 两张真实列表稳定拆出 21+29 共 50 条完整日期候选，身份 50/50；39 条取得合法完整
  四柱且 39/39 同日复算一致，冲突 0。11 条保持未完整，其中 1 条来源本身以星号隐藏
  时柱；其余 10 条需要按原图人工补录，不能用历法推算来源值。
- 续轮完成需求逐项复核，发现项目 `AGENTS.md` 仍残留 VX-09 的历史 `1900–2100`
  口径；已与领域合同、适配器和 alpha69 证据统一为 `1800–2100`。历史黄金集和
  alpha62 阶段记录仍可保留 `1900–2100`，不冒充当前公开查询边界。

### alpha70

- 修正交接判断：没有联网授权只限制自动服务适配器，不等于 VX-11 的字段导出和结果回填
  整体不可实现。首版采用无网络手动桥接，未增加网络权限、外部 SDK 或服务端依赖。
- `core:domain` 增加 `ExternalAnalysisBridge` v1：只消费
  `CaseObjectiveSummaryGenerator` 的同一五组客观字段投影，定义字段选择、默认脱敏、
  精确预览、确定性材料 ID、结构化导出/回填失败和过期命例/采用快照拒绝。
- 详情页增加“外部分析桥接”：默认选择全部字段组，隐藏身份、性别口径、精确出生时间、
  地点、经纬度和时区，并明确四柱/岁运仍属敏感资料；用户核对精确文本并主动确认后，
  App 才写入系统剪贴板并读回验证。App 不选择外部服务、不调用 AI、不自动发送。
- 用户手动粘贴结果时必须填写来源，可选填写模型并再次确认“只作为外部研究记录”；成功
  只通过既有 `TextRecordUseCase` 新增带来源、模型、材料编号、命例 revision 和边界说明的
  正式 `ANALYSIS`，计算快照、来源记录和算法真值均不改变。
- 字段选择、脱敏状态、来源/模型/结果草稿进入 `SavedStateHandle`；复制和回填确认属于
  一次性授权，Activity/进程重建后故意重置。保存期间的命例 revision 或采用快照变化会
  结构化拒绝旧材料。API 35 验收还修复了底部系统区域遮挡确认/保存触控的真实布局问题。

详细逐项证据以 `docs/REQUIREMENT_GAP_AUDIT.md` 为准。

## 6. 最新验证证据

alpha70 最终 clean 命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew clean test lint assembleDebug assembleRelease assembleDebugAndroidTest \
  --no-daemon --max-workers=1
```

结果：

- 405 个 Gradle task 成功（388 executed，17 up-to-date），耗时 4 分 4 秒；当前累计
  471 次 JVM 测试执行，零失败、零错误、零跳过。
- Lint 0 错误；app 12 条 warning、`core:data` 6 条、`core:image-parser` 2 条，
  均为已知非阻断项。
- 最终 clean alpha70 产物覆盖安装到 API 35 `emulator-5554` 后复验：VX-11 从合成命例
  进入、默认脱敏预览、剪贴板读回、Activity 重建、来源/模型/结果草稿恢复、一次性确认
  重置、Room 正式分析写入与详情读回 1/1；既有页面状态、VX-09 和候选采用恢复 4/4。
- 两张授权真实列表的一次性脱敏诊断在同一模拟器通过：分类 2/2、日期行 21+29、身份
  50/50；39 条合法完整 OCR 四柱同日一致 39/39、冲突 0、未完整 11。诊断源码、文本和
  模拟器副本均已删除，未进入 Git 或正式产物。
- Debug/Release 合并清单均为 `versionCode 71`、`0.3.0-alpha70`，且没有
  `INTERNET` 或 `ACCESS_NETWORK_STATE`。
- 设备写操作仅指向 `emulator-5554`；未触碰 OPPO。

构建产物是可再生的忽略文件，不进入 Git：

- Debug：
  `app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha70-debug.apk`
  - 56,172,203 字节
  - SHA-256
    `4e57b81d050d2d75a9cdeef9c23a5ffa32fa4222e64e174a684b1eaeb0097b6a`
- 未签名 Release：
  `app/build/outputs/apk/release/NanfengBazi-Android-v0.3.0-alpha70-release-unsigned.apk`
  - 52,302,584 字节
  - SHA-256
    `dce8f0e73de2d65560bd8ca89d06d03b828a570866f4961eee22001f85456c0b`

## 7. 尚未完成

### 可在本地继续实现

当前需求审计中没有仍可在既定边界内继续实现的本地能力。VX-01～VX-11 和核心阶段均已
完成当前自动化及 API 35 模拟器证据；不得为了保持开发进行而擅自创造新需求。

### 外部门禁

- `IM-13/QA-07`：真实问真截图迁移准确率，需要用户批准的脱敏或真实样本。
- `VX-11 自动服务适配器`：自动联网、选择具体服务或发送资料需要新的明确授权；手动
  导出/回填已完成，不再列为未实现能力。
- `QA-08`：OPPO Find N5 安装、展开/折叠、数据保留，需要用户明确授权。
- `QA-10`：正式签名、发布、GitHub Release 和回滚，需要签名材料与发布授权。

这些门禁不能用合成数据、模拟器或未签名 APK 代替。

## 8. 下一任务：等待真实样本或外部授权

### 目标

保持 alpha70 本地基线稳定。下次自动继续时，先只读复核代码、测试和需求审计；对 10 条
OCR 缺失和 1 条来源星号隐去只做带原图定位的人工确认，不允许用历法猜值。确认后再
验收 50 条身份、49 条来源可提供完整四柱的正式写入、进程重建和逐例一致性；星号行
必须保持来源未知，不能冒充完整命例。

### 可能解除门禁的输入

- 用户对当前 10 条 OCR 缺失行的逐条确认，可推进 49 条完整四柱真实列表正式迁移闭环；
  来源星号行只能保留未完整证据。
- 用户批准的基本资料、命主反馈或师傅点评真实问真样本，可推进 IM-13/QA-07 分页面验收。
- 用户明确批准自动联网、具体外部服务和发送字段后，才可在既有 VX-11 合同后增加服务
  适配器；不得绕过当前预览、默认脱敏、主动确认和来源标记。
- 用户明确授权 OPPO 与同签名安装后，才可执行 QA-08。
- 正式签名材料与发布授权齐备后，才可执行 QA-10。

### 禁止项

- 不用合成数据冒充真实问真页面准确率或 OPPO 设备证据。
- 不默认联网、调用外部 AI、发送命例资料或自行选择外部服务。
- 不接触 OPPO、不清数据、不卸载真机 App。
- 不提交真实姓名、八字、截图、密钥或构建产物。
- 不 push、不发布；没有新授权时不制造下一 alpha。

## 9. 下一轮启动检查

```bash
cd "/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi"
git rev-parse --show-toplevel
git branch --show-current
git status --porcelain=v1 | awk 'END { print "entries=" NR }'
rg -n -m 20 'VX-11|IM-13|QA-08|QA-10|外部门禁' \
  docs/REQUIREMENT_GAP_AUDIT.md docs/CURRENT_HANDOFF.md
```

若现场与本文件不一致，以现场代码和最新测试证据为准，先修正文档。无新资料或授权时，
直接保持等待，不要求用户再次发送“继续”提示，也不越过外部门禁。不要读取旧对话全文。

可直接使用的启动提示见 `docs/next-codex-prompt.md`。
