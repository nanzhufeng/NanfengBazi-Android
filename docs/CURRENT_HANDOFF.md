# 当前交接：alpha78 自适应图标安全区与下一轮入口

更新日期：2026-08-08

本文件是新 Codex 对话的唯一当前交接入口。它只保存接手所需事实，不保存旧对话过程；
历史演进以 Git、`decision-log.md` 和需求审计为准。

## 1. 一句话状态

- 仓库：`/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi`
- 当前分支：`main`
- 当前业务基线提交：`a13d623 feat: 内置本地响应式万年历` 为 alpha75 起点；alpha78 图标业务提交及交接文档以现场 `HEAD` 为准
- 版本：`0.3.0-alpha78`，`versionCode 79`
- 交接前业务工作区：干净
- 本地能力：v1 核心、VX-01～VX-11、问真式三主界面、四柱反查、本地响应式万年历，以及 UI-11 的命例、截图建档和岁运展开态均已完成当前模拟器验收
- 当前本地缺口：已清零。UI-11 已收口；八图前台批次识别曾有一次 API 35 180 秒超时，但紧接单独复跑和完整测试类均通过，尚未形成可复现实现缺陷。其余后续仍受真实样本、OPPO、正式签名或发布授权限制。

## 2. 新对话的最小读取顺序

1. 只读确认 Git 根目录、分支、HEAD 和工作区文件数量。
2. 完整读取根目录 `AGENTS.md`。
3. 完整读取本文件。
4. 读取 `docs/next-codex-prompt.md`。
5. 只读取当前任务涉及的：
   - `docs/REQUIREMENT_GAP_AUDIT.md`
   - `docs/architecture-governance.md`
   - `docs/domain-rules.md`
   - `docs/TEST_STRATEGY.md`
   - `docs/decision-log.md`
6. 再检查现场代码和最新测试证据；不要读取旧对话全文。

现场代码与最新测试证据优先于本文件。发现不一致时，先报告并修正文档，再继续开发。

## 3. 产品与架构基线

### 产品边界

- 三个主入口固定为：排盘、记录、设置。
- 首页承担出生输入、即时排盘入口、四柱反查入口和本地万年历入口。
- 记录页展示全部保存案例；点击后进入同一命例的四标签详情。
- 详情四标签：基本信息、基本排盘、专业细盘、断事笔记。
- 时间、地区、四柱、年份范围和时区优先使用联动滚轮，避免不必要的手工输入。
- 删除只复述操作方式或当前状态的辅助小字；保留风险、证据、异常和业务判断信息。

### 唯一真值与模块边界

- UI 不直接调用 Tyme4j，也不自行复制历法或四柱算法。
- `core:domain` 定义公开合同、模型和结构化错误。
- `core:engine-tyme` 是 Tyme4j 生产适配器。
- `core:data` 负责 Room、附件、备份和持久化。
- 测试差分引擎只存在于测试依赖，不得进入生产入口。
- Tyme4j 全局 `LunarHour.provider` 只允许在四柱反查适配器中受锁访问，并在成功、失败、取消和并发路径恢复原对象。

### 主要公开合同

| 能力 | 领域入口 | 生产实现/事实源 |
|---|---|---|
| 正向排盘 | `BaziEngine` | `TymeBaziEngine` |
| 四柱反查 | `FourPillarsLookup` | `TymeFourPillarsLookup` |
| 本地万年历 | `AlmanacReader` | `TymeAlmanacReader` |
| 命例管理 | 领域用例与仓储合同 | Room 仓储实现 |
| 截图导入 | 图片会话、候选与冲突合同 | bundled OCR + 本地解析器 |
| 客观摘要 | 摘要合同 | 唯一采用快照与正式记录 |
| 图片导出/分享 | 图片文档合同 | 同一渲染字节 |
| 外部分析桥接 | `ExternalAnalysisBridge` | 本地手动导出/回填，不含自动联网客户端 |

## 4. 已完成的当前成果

### alpha74：界面与输入体系收口

- 首页、记录、设置按问真同类信息架构重构，并使用南枫产品视觉规范。
- 首页输入卡已重新校准排版、比例、层级和主次按钮。
- 日期、地区、四柱、年份范围、时区均为滑动/联动选择；选项切换带轻触觉反馈。
- 首页重复的“即时排盘 / 四柱反查 / 详细设置”快捷卡已删除。
- 首页“四柱”分段直接打开四柱选择器；子时口径迁入设置页。
- 案例列表使用真实姓名式预览数据，不再使用时间戳假名或重复同一八字。
- 星座列表项为“圆形星座符号 + 圆下方完整名称（如处女座）”，比例保持克制。
- App 图标使用用户提供的原始资源并保持图形保真。
- 详情四个标签共用固定身份头和同一命例/采用快照，不形成第二算法真值。

### alpha75：本地响应式万年历

- 首页下方新增真实可操作入口，不嵌入外部网页。
- `core:domain` 定义 `AlmanacReader`、日期、月视图、日详情和结构化错误。
- `core:engine-tyme` 通过 `TymeAlmanacReader` 本地生成数据。
- 月视图固定 42 格，包含公历、农历、节气、节日和日柱；日期详情可复算。
- “用于排盘”只把所选日期带回既有出生表单，最终仍经过唯一排盘引擎。
- 手机使用单列；宽度 `>=760dp` 使用双栏月历/详情。
- 支持范围为 1800～2100。
- App 运行时没有 `INTERNET` 权限，不含 WebView 或外部 AI 自动调用。

### alpha76：UI-11 展开态

- 宽屏截图审阅左侧固定同一私有原图及来源框，右侧仍使用原有候选和人工确认字段；不新增 OCR、历法或第二数据真值。
- 宽屏岁运在同一已采用 `CalculationResult` 下拆为“概览”和“大运—流年详情”；窄屏继续单列。
- 仅当全屏达到 `840dp` 且嵌套详情剩余至少 `360dp` 时启用这两组双栏，避免导航轨与命例索引占宽后错误按全屏阈值判断。

### alpha77：用户提供启动图标

- `design/assets/app-icon-master.jpg` 是唯一当前母版；`app_icon_source`、五档 `mipmap-*` 兼容图标及圆形图标均直接由该文件缩放生成。
- API 35 的系统应用信息页和 Pixel Launcher 应用抽屉已实测显示；其后 ColorOS 实测发现直接使用满画布前景会被自适应遮罩二次放大。

### alpha78：自适应图标安全区校正

- 自适应前景从同一 1254×1254 母版缩放至 `60%` 后置于白色 1254×1254 画布中央；这是抵消 Android 启动器安全区归一化的包装校正，不重绘、裁切或改变原图比例。
- 五档 `mipmap-*` 兼容图标保持由母版直接导出，避免低 API 或非自适应路径发生不必要的缩小。
- OPPO Find N5 / ColorOS 已在系统应用详情页与桌面实际复验：四边留白完整，文字、四柱、金色圆点和枫叶均未丢失。真机截图仅作临时本机核验，不进入仓库。

### v1 与 VX 能力

- 命例创建、编辑、复制、软删除、回收站恢复、检索、筛选、排序和最近查看。
- 正向排盘、农历输入、IANA 时区、两种子时口径、真太阳时显式口径和边界处理。
- 大运、流年、流月、流日、流时及客观证据展示。
- 图片导入、可恢复 OCR 会话、候选/冲突人工确认和来源保留。
- 单命例交换、完整备份、密码加密、附件事务和恢复预演。
- 图片导出、长图分享、命例对比、客观摘要和本地手动外部分析桥接。
- VX-09 四柱反查：1800～2100、IANA 时区、两种子时口径、逐候选正向复算；候选只代表可解释的民用时刻，不证明出生分钟唯一，也不静默推算真太阳时。

## 5. alpha75 冻结验证证据

- 强制全量 Gradle：`399/399` tasks，`BUILD SUCCESSFUL in 3m10s`。
- JVM 聚合：`485` tests，`0` failures，`0` errors，`0` skipped。
- 设备：仅 `emulator-5554`，Android API 35。
- 自动设备流：`AutomatedPickerFlowTest OK (3 tests)`，覆盖日期/地点、四柱/年份/时区和万年历带回排盘。
- Debug APK 安装后，设备 `base.apk` 读回哈希与本地产物完全一致。
- Debug SHA-256：`688a855e35ac30bedbc58233d591d753ca75332cf9f5593ccef4822a88cdc763`
- 未签名 Release SHA-256：`8f7a34f265e96c0f4deee21c9d674f37622cfd14df7dc1c24b61049f18a1272d`
- 视觉证据：
  - `design/qa/alpha75/home.png`
  - `design/qa/alpha75/almanac-mobile.png`
- 未操作 OPPO，未 push，未发布。

本轮若只修改交接文档，不重复运行全量构建；业务代码变化后才按风险重新取证。

## 5A. alpha76 定向验证证据

- 离线 `test lintDebug assembleDebug assembleRelease assembleDebugAndroidTest` 已完成；当前 JVM XML 聚合 `208` tests，`0` 个失败/错误 XML，Debug Lint 无 Error/Fatal。
- `emulator-5554`，Android API 35：`StageTwoFlowTest#lateRatHourRuleReachesVersionedInstantChart` 通过，覆盖即时排盘、保存、岁运详情和展开态语义节点。
- 同设备单图分享审阅流 `ScreenshotShareFlowTest#系统分享合成问真列表图后私有复制并完成离线识别` 通过，覆盖私有复制、离线识别、审阅字段和展开态来源图语义节点。
- 临时将模拟器从 `1140×2616` 覆盖为 `2400×2616` 以进入宽屏配置，以上两条再次通过后已执行 `wm size reset` 恢复；这不构成 OPPO 证据。
- 同类的八张图片后台识别用例本轮在 `180s` 等待 `NEEDS_REVIEW` 超时；它未被修改或标为通过，需在独立的导入可靠性任务中复现和处理。
- Debug SHA-256：`298fcb111e9d09b87ace272f49180d5761168ea4dc0ba4ade8cdc888729aab4f`；已安装 API 35 的 `base.apk` 与该产物逐字节一致。
- 未签名 Release SHA-256：`ceeb7adfe8c79da747fd1352f47f63f2f381c4766af42c3d1d8a3896ee749f51`。

## 5B. alpha77 启动图标验证证据

- `:app:lintDebug :app:assembleDebug` 通过，Debug Lint 无 Error/Fatal。
- `emulator-5554` API 35 已覆盖安装 `0.3.0-alpha77` / `versionCode 78`；系统应用信息页和 Pixel Launcher 应用抽屉均实际显示新图标。
- Debug SHA-256：`d489b2bdab07dc1d7bb670f559534ce46007cfd9cd16d2dfe0c8dc3a798f4d6b`；设备 `base.apk` 读回哈希完全一致。
- 未操作 OPPO、未清除设备数据、未使用正式签名、未 push 或发布。

## 5C. alpha78 自适应图标安全区验证证据

- `:app:lintDebug :app:assembleDebug` 通过，Debug Lint 无 Error/Fatal。
- OPPO Find N5 / ColorOS 已同签名覆盖安装 `0.3.0-alpha78` / `versionCode 79`；系统应用详情页与桌面均实测显示完整图形及留白。
- Debug SHA-256：`1ce44f77086e6eb16e3376918d38d68fbd143d8ffcb2fddee761c8c142eb4455`；设备 `base.apk` 读回哈希完全一致。
- 未清除设备数据、未使用正式签名、未 push 或发布。

## 6. 尚未完成：间歇性观测与外部门禁

### 已完成的本地展开态（UI-11）

- 宽度 `>=840dp` 的导航轨、命例索引—详情双栏、截图“原图—字段”和岁运“大运—流年详情”均已有代码和 API 35 临时展开尺寸回归。
- 这只是本地模拟器布局证据；不能替代 OPPO 折叠/展开、马达、数据保留或真实宽屏视觉验收。

### 批量识别间歇性观测

- 首次完整类运行时，八图用例在 API 35 等待 `NEEDS_REVIEW` 180 秒超时；之后单独复跑通过，完整 `ScreenshotShareFlowTest` 又以 `3/3` 通过，未稳定复现。
- 现阶段不以该单次超时改动生产逻辑、放宽断言或宣称性能结论；若再次发生，先保留 WorkManager、会话状态和通知授权分支诊断，再决定是否进入本地修复。

### 真实问真样本

- `IM-13 / QA-07`：当前两张授权用户列表已有 50 条日期候选、身份 50/50；39 条合法完整 OCR 四柱同日一致，0 冲突。
- 仍有 10 条 OCR 缺失和 1 条来源星号隐去，必须结合用户批准的原图逐条确认；不得用历法猜值补齐。
- 基本资料、专业细盘、命主反馈和师傅点评的更多真实页面类型，仍需用户批准的脱敏样本。
- `CH-04 / FT-07 / FT-08` 的真实问真边界比较同样依赖批准样本。

### 真机

- `QA-08`：OPPO Find N5 安装、折叠/展开、数据保留、TalkBack、触觉和真实宽屏视觉。
- `UI-11` 的宽屏分支与状态已有自动化；不能把模拟器或代码分支冒充 OPPO 实机证据。
- 已获 OPPO 操作授权；alpha77→alpha78 已同签名覆盖且未清数据。折叠/展开、TalkBack、触觉与真实宽屏视觉仍未完成，不能由当前外屏图标验收替代。

### 签名与发布

- `QA-10`：正式签名、同签名覆盖、GitHub Release、回滚和正式产物冻结。
- 需要正式签名材料与明确发布授权；密钥不得进入仓库。
- 未授权时不 push、不发布。

### 外部 AI 自动服务

- VX-11 的本地手动导出/回填已完成，不是未实现能力。
- 自动联网、选择服务、发送字段或接入外部 AI 是新的产品授权，不能从“继续”推断。

## 7. 下一轮默认动作

1. 完成第 2 节的最小读取和只读现场核对。
2. 检查 `REQUIREMENT_GAP_AUDIT` 是否存在仍可在既定边界内完成的真实本地缺口。
3. 若现场已覆盖该缺口，跳过，不重复实现；先修正文档。
4. 若没有新的样本、真机或发布授权，保持 alpha78，不为了制造进度擅自增加功能或升级 alpha；八图超时仅在再次发生时作为复现诊断任务。
6. 用户给出新的明确产品任务时，从当前冻结基线继续，并只在 `emulator-5554` API 35 验收，除非用户另行扩展设备范围。

本项目不要求用户再次粘贴旧交接。新对话直接使用 `docs/next-codex-prompt.md` 中的提示词即可。

## 8. 安全启动命令

```bash
cd "/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi"
git rev-parse --show-toplevel
git branch --show-current
git rev-parse --short HEAD
git status --porcelain=v1 | awk 'END { print "entries=" NR }'
```

禁止裸跑未知规模的 `git status/diff/log`。先计数或 `--stat`，再限定文件查看。

## 9. 关键文件索引

- 执行契约：`AGENTS.md`
- 下一轮提示词：`docs/next-codex-prompt.md`
- 需求与差距：`docs/REQUIREMENT_GAP_AUDIT.md`
- 架构边界：`docs/architecture-governance.md`
- 领域规则：`docs/domain-rules.md`
- 测试门禁：`docs/TEST_STRATEGY.md`
- 决策记录：`docs/decision-log.md`
- 产品需求：`docs/PRODUCT_REQUIREMENTS.md`
- alpha75 视觉证据：`design/qa/alpha75/`
