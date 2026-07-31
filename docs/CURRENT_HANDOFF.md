# 当前交接：alpha66 师傅点评观点候选

更新日期：2026-07-31

## 1. 接手快照

- 项目：南枫八字，本地优先 Android App。
- 仓库：`/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi`
- 当前分支：`main`
- alpha66 本地代码基线：本文件所在提交；上一基线为
  `9223f92 feat: add objective chart summary`。
- 版本：`versionCode 67`，`versionName 0.3.0-alpha66`
- 本轮开始前工作区干净；接手时仍须现场复查当前提交和工作区。
- alpha66 验收时 ADB 仅 `emulator-5554` 在线，API 35。
- 本轮点评候选设备验收只使用合成命例；用户真实问真原图、OCR 原文、姓名、生日和
  四柱均未进入 Git、剪贴板夹具或截图证据。
- 本轮未操作 OPPO、网络、外部 AI、远端推送或发布。

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

详细逐项证据以 `docs/REQUIREMENT_GAP_AUDIT.md` 为准。

## 6. 最新验证证据

alpha66 clean 命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew clean test lint assembleDebug assembleRelease assembleDebugAndroidTest \
  --max-workers=1
```

结果：

- 405 个 Gradle task 成功（388 executed，17 up-to-date）；431 次 JVM 测试执行
  零失败、零错误、零跳过。
- Lint 0 错误；app 12 条 warning、`core:data` 6 条、`core:image-parser` 2 条，
  均为已知非阻断项。
- API 35 `emulator-5554` 点评候选主流程与三个独立 SavedState 场景合并复验 4/4
  通过：正式点评进入候选页，拒绝一条、编辑并改类后采用一条，Activity 重建保留审核
  状态，返回详情可见原点评未变且正式分析新增。
- 1140×2616 候选页截图完成视觉检查：标题、规则/来源说明、非算法真值提示、原文区间、
  编辑框、横向分类、规则证据和采用/拒绝动作层级清楚；截图仅存临时目录并已删除。
- Debug/Release 合并清单均为 `versionCode 67`、`0.3.0-alpha66`，且没有
  `INTERNET` 或 `ACCESS_NETWORK_STATE`。
- 设备证据仅来自 `emulator-5554`，不等于 OPPO 真机或真实问真样本验收。

构建产物是可再生的忽略文件，不进入 Git：

- Debug：
  `app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha66-debug.apk`
  - 56,057,515 字节
  - SHA-256
    `1193f5e5a9fc78b7d93e41172233a9c4de10034ef11ea89dd72c3ca4854dedbd`
- 未签名 Release：
  `app/build/outputs/apk/release/NanfengBazi-Android-v0.3.0-alpha66-release-unsigned.apk`
  - 52,220,664 字节
  - SHA-256
    `5fd328b894f7e3b412f622b986b323d4e59b6501cd17e237afb72458ef43e9fd`

## 7. 尚未完成

### 可在本地继续实现

建议一次只完成一个增量：

1. `VX-05` 命主反馈主题标签候选增强。

### 外部门禁

- `IM-13/QA-07`：真实问真截图迁移准确率，需要用户批准的脱敏或真实样本。
- `VX-11`：外部 AI 导出/回填，需要新的联网、字段范围和脱敏授权。
- `QA-08`：OPPO Find N5 安装、展开/折叠、数据保留，需要用户明确授权。
- `QA-10`：正式签名、发布、GitHub Release 和回滚，需要签名材料与发布授权。

这些门禁不能用合成数据、模拟器或未签名 APK 代替。

## 8. 下一唯一任务：命主反馈主题标签候选增强

### 目标

实现 `VX-05` 命主反馈主题标签候选增强；必须保留完整命主反馈和既有事件事实，以本地
确定性规则提取可解释、可编辑、可逐条确认的主题标签候选，不自动覆盖来源或替用户
确认主题。

### 建议所有权

- 在 `core:domain` 定义版本化反馈主题候选、来源定位、规范标签、规则证据、审核状态和
  结构化失败；提取器使用公开接口，页面不内置关键词规则。
- 确定性解析适配放在合适的解析层，复用既有 `OWNER_FEEDBACK` 与事件分类事实；不得把
  候选主题冒充用户已经确认的标签。
- 只有逐条采用才进入现有正式标签/分类聚合边界；拒绝、编辑和恢复不得覆盖反馈原文、
  事件或其版本历史。

### 当前已知边界

- 首版只做可解释的主题标签建议，不生成新的命理结论或事件事实。
- 空反馈、错误来源类型、无可解释候选和规则版本不支持必须分开返回。
- 候选必须幂等、去重，并能在来源内容或 revision 变化后判定过期。

### 最小验收

1. 领域合同覆盖错误来源、空反馈、无候选、多候选、重复标签、来源定位和版本过期。
2. 相同来源重复提取得到相同候选；每个候选保留可解释的来源证据。
3. UI 支持逐条采用、编辑和拒绝；任何动作都不覆盖完整反馈或事件历史。
4. API 35 模拟器从正式命主反馈进入候选页，确认一条后写入正式标签/分类并在重建后
   读回。
5. 更新受影响治理文档、下一 alpha、全量构建和本地准确提交。

### 禁止项

- 不自动确认候选、不覆盖反馈原文、不把主题候选称为用户事实或算法真值。
- 不默认联网、调用外部 AI 或发送命例资料。
- 不接触 OPPO、不清数据、不卸载真机 App。
- 不提交真实姓名、八字、截图、密钥或构建产物。

## 9. 下一轮启动检查

```bash
cd "/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi"
git rev-parse --show-toplevel
git branch --show-current
git status --porcelain=v1 | awk 'END { print "entries=" NR }'
rg -n -m 20 '反馈主题标签|VX-05' docs app/src core --glob '!**/build/**'
```

若现场与本文件不一致，以现场为准，先修正文档再实现。不要读取旧对话全文。

可直接使用的启动提示见 `docs/next-codex-prompt.md`。
