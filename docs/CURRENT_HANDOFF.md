# 当前交接：Stage 4A 公农历输入第一增量

更新日期：2026-07-30

## 当前结论

- Stage 0、Stage 1、Stage 2、Stage 3A、Stage 3B 均已完成当前实现与自动化证据。
- Stage 4A 第一增量已完成：公历、农历、闰月可以手动新建和编辑，唯一计算入口会同时
  输出标准公历与标准农历转换证据。
- 下一安全增量是 Stage 4A 的地区、经纬度、时区/UTC offset 与 DST 歧义确认；随后才是
  独立版本化真太阳时组件和更多基础排盘字段。
- 问真输出仍是截图迁移的首要验收标准；算法真值仍由版本化规则和边界测试负责，二者
  不得混用。
- 未使用真实问真资料、真实姓名或用户截图；未安装或操作 OPPO，未 push、未发布。

## 本增量实现

### 输入与编辑

- `CaseFormState` 增加显式历法与闰月状态；页面提供“公历 / 农历 / 闰月”选择。
- 闰月只在农历模式出现，领域层以 `isLeapMonth` 表达，不允许页面或数据层使用负月份。
- 公历继续通过 `LocalDateTime` 校验真实日期；农历先校验年月日时分秒基础范围，再由
  Tyme4j 校验具体年份的闰月和当月天数。
- 农历命例进入编辑器时原样回显历法、月份和闰月状态，不自动改写为公历输入。

### 唯一算法入口与转换证据

- `TymeBaziEngine` 同时接受 `BirthCalendarInput.Solar` 与
  `BirthCalendarInput.Lunar`。
- 适配器按 Tyme4j 官方约定，只在调用 `LunarHour.fromYmdHms` 时把闰月映射为负月份，
  再通过 `getSolarTime` 得到统一计算时刻。
- 公历输入先生成对应农历，农历输入先生成对应公历；两条路径最终使用同一
  `SolarTime`、`LunarHour` 和起运计算。
- `CalculationResult.calendarConversion` 记录输入历法、标准公历和标准农历；
  `normalizedInput` 继续保留用户原始历法输入。
- 列表出生时间排序、重复候选、单命例冲突和完整备份冲突优先使用标准公历证据，使同一
  时刻的公历/农历命例进入同一时间轴；旧快照无证据时才回退原始历法值。
- 转换证据字段带默认空值，旧 Room 快照、单命例文件与完整备份缺少该字段时仍可读取，
  不伪造历史转换结果。

### 页面与错误语义

- 原始录入区明确显示“公历”或“农历”，农历闰月显示“闰”。
- 计算结果区显示“换算公历”和“换算农历”，便于后续问真截图迁移逐项核对。
- 年份不存在所选闰月、日期超出农历当月天数等情况统一返回中文错误，计算失败时不会
  写入命例。
- 真太阳时仍明确拒绝，不随农历支持静默开启。

## 所有者与边界

- 唯一计算入口：`BaziEngine.calculate()`。
- Tyme4j 类型与负闰月约定只存在于 `core:engine-tyme`。
- 唯一命例写入口：`CaseRepository`。
- 页面不直接调用 Tyme4j、不自行换算历法、不解析交换协议。
- 仓库不得提交用户真实八字、姓名、截图、密钥或构建产物。

## 当前验证证据

全量命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test lint assembleDebug assembleRelease
```

- 104 条唯一单元契约；Debug/Release 变体合计 191 次执行，0 失败、0 跳过。
- 新增自动化覆盖：
  - 农历字段基础范围；
  - 公历 2023-01-22 13:00 与农历 2023 年正月初一 13:00 的四柱和起运等价；
  - 2023 年闰二月初一转换为 2023-03-22，并保留闰月证据；
  - 2022 年不存在闰二月时中文失败；
  - 农历表单与闰月编辑回显；
  - 跨历法出生时间排序与同一出生时刻重复候选；
  - 缺少转换字段的旧计算快照继续读取。
- App 与 `core:data` Lint 均 0 错误；仅有 9 + 5 条依赖版本提示。
- Debug 与未签名 Release 均构建成功。
- API 35 模拟器 `ExpenseCapture_API35`：
  - 新增历法切换与农历新建/保存/详情换算证据 2/2 通过；
  - 既有 Stage 2/3 长系统文件主流程冷启动复核 1/1 通过；
  - 首次合并运行中既有长流程遇到 DocumentsUI 返回超过 10 秒，测试等待调整为 30 秒；
    一次单独重跑的 instrumentation 进程被模拟器 SIGKILL，日志无 App 异常；冷启动并
    预热 DocumentsUI 后最终通过；
  - 全部设备测试仅在 `emulator-5554` 执行，未触碰 OPPO。
- 真实问真迁移仍未执行；自动化证据不能替代最终隐私批准样本验收。

## APK

Debug 验收构建：

`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha23-debug.apk`

- 大小：10,319,416 bytes
- SHA-256：`74935de16fb4b409d93c4bb6ec5ab520938492888d3f30f7ce6c5b3d88fb1d6b`

未签名 Release：

`app/build/outputs/apk/release/NanfengBazi-Android-v0.3.0-alpha23-release-unsigned.apk`

- 大小：6,807,329 bytes
- SHA-256：`70740085c0bb3fbef13fa6c7e95c0ee72c2488a785102357a4d49da96d8e8097`

## 当前限制与风险

- 地区、经纬度输入、时区历史、UTC offset 固化、DST 重叠/不存在时刻确认与真太阳时尚未
  接通。
- 生肖、节气、十神、藏干、纳音、十二长生、空亡等基础排盘字段尚未完整输出。
- 问真截图导入、离线 OCR、页面分类、多图归组与可恢复导入会话尚未实现。
- 软删除没有永久清理入口，这是数据安全选择；正式清理仍需用户可验证备份和附件引用计数。
- App 仍是手机单列工作台，OPPO Find N5 展开双栏、无障碍和大字体尚未验收。
- 当前 Debug APK 不是正式签名 Release；OPPO 数据保留安装与发布需要用户明确授权。

## 下一安全增量

继续 Stage 4A，优先顺序：

1. 接通出生地区、经纬度、IANA 时区、解析 UTC offset 与时区数据版本；
2. 对 DST 重叠时刻要求用户明确选择 offset，对不存在时刻明确失败；
3. 实现独立版本化真太阳时组件，覆盖跨日和跨时辰边界；
4. 扩展生肖、节气、十神、藏干、纳音、长生、空亡等基础排盘结果；
5. 扩大黄金边界集，再进入 Stage 4B 的 50+ 样本门禁。

`docs/REQUIREMENT_GAP_AUDIT.md` 是 v1.0 的逐项事实清单；Stage 4A 第一增量完成不等于
整个产品已经落地。
