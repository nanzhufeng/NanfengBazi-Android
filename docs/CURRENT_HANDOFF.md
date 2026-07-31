# 当前交接：alpha61 后续外围能力

更新日期：2026-07-31

## 1. 接手快照

- 项目：南枫八字，本地优先 Android App。
- 仓库：`/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi`
- 当前分支：`main`
- alpha61 生产代码基线：`12cfdbe feat: compare case archives objectively`
- 版本：`versionCode 62`，`versionName 0.3.0-alpha61`
- 交接整理开始前工作区：干净，未提交/未跟踪文件 0；接手时仍须现场复查。
- 当前 ADB：仅 `emulator-5554` 在线。
- 本轮交接不修改生产代码、不操作真实用户数据、不推送远端。

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

详细逐项证据以 `docs/REQUIREMENT_GAP_AUDIT.md` 为准。

## 6. 最新验证证据

alpha61 clean 命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew clean test lint assembleDebug assembleRelease assembleDebugAndroidTest \
  --max-workers=1
```

结果：

- 405 个 Gradle 任务成功。
- 368 次单元测试执行，0 failure、0 error、0 skipped。
- Lint 0 error，19 条依赖版本 warning。
- API 35 模拟器命例对比长流程和 `SavedStateHandle` 恢复组合：2/2 通过。
- 设备证据仅来自 `emulator-5554`，不等于 OPPO 真机或真实问真样本验收。

构建产物是可再生的忽略文件，不进入 Git：

- Debug：
  `app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha61-debug.apk`
  - 55,860,291 bytes
  - SHA-256 `716b00190272e1d7cedb51dc6a1a2f7e762f9f02920a42235608967b2e8eea6d`
- 未签名 Release：
  `app/build/outputs/apk/release/NanfengBazi-Android-v0.3.0-alpha61-release-unsigned.apk`
  - 52,088,962 bytes
  - SHA-256 `3788b23fea24f2b36c64d60d1bd25b08da067d6ad71afbc61f5e7805c110963f`

## 7. 尚未完成

### 可在本地继续实现

建议一次只完成一个增量：

1. `VX-09` 四柱反查。
2. `VX-07` 图片导出与 `VX-08` 长图分享，共用版本化渲染结果。
3. `VX-10` 客观命盘摘要，只消费确定性字段和证据。
4. `VX-04` 师傅点评观点候选增强。
5. `VX-05` 命主反馈主题标签候选增强。

### 外部门禁

- `IM-13/QA-07`：真实问真截图迁移准确率，需要用户批准的脱敏或真实样本。
- `VX-11`：外部 AI 导出/回填，需要新的联网、字段范围和脱敏授权。
- `QA-08`：OPPO Find N5 安装、展开/折叠、数据保留，需要用户明确授权。
- `QA-10`：正式签名、发布、GitHub Release 和回滚，需要签名材料与发布授权。

这些门禁不能用合成数据、模拟器或未签名 APK 代替。

## 8. 下一唯一任务：四柱反查

### 目标

输入年柱、月柱、日柱、时柱、起止年份、IANA 时区和子时口径，返回可解释、可复算的
公历候选时刻；首个版本只支持民用时，不静默推算真太阳时。

### 建议所有权

- 在 `core:domain` 定义查询、候选、结构化错误和公开接口。
- 在 `core:engine-tyme` 实现 Tyme 适配器。
- `app` 只协调查询、保存可恢复表单状态并展示结果。
- 页面不得直接依赖 Tyme4j。

### 已发现但尚未实现的技术事实

- Tyme4j 1.5.1 的 `com.tyme.eightchar.EightChar#getSolarTimes(int, int)` 能生成候选。
- 该方法内部会读取进程级 `LunarHour.provider`；两种子时口径必须在
  `core:engine-tyme` 内串行切换并在成功或异常后恢复，不能从 UI 直接调用。
- 每个候选还应通过项目唯一规则入口复核四柱；不能把库返回列表未经验证直接展示。
- 必须先冻结年份范围上限、IANA 时区/DST 语义、子时口径和“候选代表时刻”文案。

### 最小验收

1. 领域校验：四柱必须是有效干支，起止年有界且顺序正确，时区有效。
2. 引擎测试：已知命例正向计算后能反查回来；无解、跨 60 年周期和两种子时口径覆盖。
3. 全局 provider 在并发、异常和取消后恢复，不污染后续正向计算。
4. UI 可输入、搜索、显示候选/无解/错误；明确候选不是出生分钟的唯一证明。
5. 页面、表单和查询参数进入 `SavedStateHandle` 恢复合同。
6. API 35 模拟器完成真实输入—查询—结果—重建流程。
7. 更新需求审计、架构所有权、领域规则、测试策略、决策日志和本交接。

### 禁止项

- 不引入第二生产历法真值。
- 不保存查询结果为正式命例，除非后续有单独确认流程。
- 不默认联网、调用外部 AI 或发送出生资料。
- 不接触 OPPO、不清数据、不卸载真机 App。
- 不提交真实姓名、八字、截图、密钥或构建产物。

## 9. 下一轮启动检查

```bash
cd "/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi"
git rev-parse --show-toplevel
git branch --show-current
git status --short
rg -n -m 20 '四柱反查|VX-09' docs app/src core --glob '!**/build/**'
```

若现场与本文件不一致，以现场为准，先修正文档再实现。不要读取旧对话全文。

可直接使用的启动提示见 `docs/next-codex-prompt.md`。
