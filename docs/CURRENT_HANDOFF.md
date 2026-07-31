# 当前交接：alpha62 后续外围能力

更新日期：2026-07-31

## 1. 接手快照

- 项目：南枫八字，本地优先 Android App。
- 仓库：`/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi`
- 当前分支：`main`
- alpha62 本地代码基线：本文件所在提交；上一基线为
  `12cfdbe feat: compare case archives objectively`。
- 版本：`versionCode 63`，`versionName 0.3.0-alpha62`
- 本轮开始前工作区干净；接手时仍须现场复查当前提交和工作区。
- alpha62 验收时 ADB 仅 `emulator-5554` 在线，API 35。
- 本轮未操作真实用户数据、OPPO、网络、外部 AI、远端推送或发布。

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

详细逐项证据以 `docs/REQUIREMENT_GAP_AUDIT.md` 为准。

## 6. 最新验证证据

alpha62 clean 命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew clean test lint assembleDebug assembleRelease assembleDebugAndroidTest \
  --max-workers=1
```

结果：

- 405 个 Gradle 任务成功。
- 385 次单元测试执行，0 failure、0 error、0 skipped。
- Lint 0 error；app 12 条 warning，两个支撑模块共 8 条 warning。
- API 35 `emulator-5554` 四柱反查真实输入—查询—候选—免责声明—Activity 重建，
  以及独立 `SavedStateHandle` 重建组合：2/2 通过。
- 设备证据仅来自 `emulator-5554`，不等于 OPPO 真机或真实问真样本验收。

构建产物是可再生的忽略文件，不进入 Git：

- Debug：
  `app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha62-debug.apk`
  - 55,909,443 bytes
  - SHA-256 `5cdbb1ef6a46f0c98cd3fbc9e998d3d456ad280f6b5a1b3e95e49e286792805a`
- 未签名 Release：
  `app/build/outputs/apk/release/NanfengBazi-Android-v0.3.0-alpha62-release-unsigned.apk`
  - 52,121,730 bytes
  - SHA-256 `f41869331c3df37c983ae2ad1a4f1b54423566e08ad6457a4eb815d9b042c27f`

## 7. 尚未完成

### 可在本地继续实现

建议一次只完成一个增量：

1. `VX-07` 图片导出与 `VX-08` 长图分享，共用版本化渲染结果。
2. `VX-10` 客观命盘摘要，只消费确定性字段和证据。
3. `VX-04` 师傅点评观点候选增强。
4. `VX-05` 命主反馈主题标签候选增强。

### 外部门禁

- `IM-13/QA-07`：真实问真截图迁移准确率，需要用户批准的脱敏或真实样本。
- `VX-11`：外部 AI 导出/回填，需要新的联网、字段范围和脱敏授权。
- `QA-08`：OPPO Find N5 安装、展开/折叠、数据保留，需要用户明确授权。
- `QA-10`：正式签名、发布、GitHub Release 和回滚，需要签名材料与发布授权。

这些门禁不能用合成数据、模拟器或未签名 APK 代替。

## 8. 下一唯一任务：图片导出与长图分享

### 目标

实现 `VX-07` 图片导出和 `VX-08` 长图分享；两者必须消费同一版本化渲染结果，导出的
内容与当前已采用快照及正式研究资料一致，并通过 Android 系统文件/分享入口真实交付。

### 建议所有权

- 先在 `core:domain` 定义版本化导出输入、渲染事实和结构化失败。
- `app` 只负责 Compose/Android 渲染适配、系统文件写入和分享 Intent。
- 图片导出与长图分享必须复用同一渲染服务，不得各自拼接第二套展示真值。
- 来源截图证据与本机计算值继续分离；不得把未采用候选或旧快照冒充当前盘。

### 当前已知边界

- 当前没有正式图片渲染合同；不得直接截图当前可见 Compose 视口冒充完整长图。
- 导出必须明确版本、范围、隐私提示和失败恢复；系统分享目标属于外部条件。
- 首版只导出已经存在的确定性字段和正式记录，不自动生成吉凶、合婚或主观结论。

### 最小验收

1. 版本化渲染输入和输出合同覆盖已采用快照、缺失字段和长内容。
2. 图片导出与分享使用同一渲染字节，尺寸、分页/长图和中文字体可验证。
3. 系统文件写入失败、分享目标不存在和用户取消均有结构化结果且不丢当前页面状态。
4. API 35 模拟器完成真实命例—导出—系统文件读回，以及长图分享 Intent 流程。
5. 更新受影响治理文档、下一 alpha、全量构建和本地准确提交。

### 禁止项

- 不重新计算或改写已采用快照，不把来源截图值覆盖本机真值。
- 不默认联网、调用外部 AI 或发送命例资料。
- 不接触 OPPO、不清数据、不卸载真机 App。
- 不提交真实姓名、八字、截图、密钥或构建产物。

## 9. 下一轮启动检查

```bash
cd "/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi"
git rev-parse --show-toplevel
git branch --show-current
git status --short
rg -n -m 20 '图片导出|VX-07' docs app/src core --glob '!**/build/**'
```

若现场与本文件不一致，以现场为准，先修正文档再实现。不要读取旧对话全文。

可直接使用的启动提示见 `docs/next-codex-prompt.md`。
