# 南枫八字现行产品合同

> 以当前生产实现和适用验证为依据。合同路由见[索引](CONTRACT_INDEX.md)，原始阶段规划完整保留于[历史需求](history/contracts/PRODUCT_REQUIREMENTS-before-20261012.md)。历史规划不再作为已实现功能的排除项，也不自动授权未来功能。

## 产品目标与边界

本地优先的Android排盘与命例研究：可靠录入、版本化排盘、记录检索、资料保真、备份恢复。核心排盘/命例管理不依赖账号或API；用户可选择AI、视觉识别和加密云备份。当前Android实现不代表iOS/Windows实现。

算法准确性和资料迁移分别验收：截图一致不是天文/算法正确证明；来源、计算、采用值分开，不把缺失资料拼造成确定事实。命理/AI解释属于研究与文化娱乐，不生成诊断、投资建议或确定人生事件。

## 已有功能及生产入口

| 用户任务 | 现有实现/主要入口 | 产品约束 |
| --- | --- | --- |
| 出生录入/即时排盘 | CaseWorkflow、BirthInputPickers、BaziEngine | 公历/农历/闰月/四柱反查/地区时区/精度与来源；预览与保存分离 |
| 基础排盘与岁运 | TymeBaziEngine、ProfessionalFortuneResolver | 采用同一版本快照；精确节令和日界，规则详见domain-rules |
| 专业细盘操作 | StageTwoViewModel、FortuneDetailsView | 五层候选、观察时刻、列表点选/滚动/标签/宽窄恢复；保持正确既有结果 |
| 命例管理 | CaseRepository、CaseManagement、RecordHubScreen | 用户/名人库、分组标签、筛选排序、置顶、回收站、恢复和永久删除为不同动作 |
| 断事笔记与事件 | CaseNotesEditorUseCase、CaseEventUseCase | 反馈/师傅/AI分开、版本历史、caseId+revision，编辑失败保留输入 |
| 万年历与用日期排盘 | TymeAlmanacReader、AlmanacFeature | 本地历法事实，所选日期进入既有输入/预览链路 |
| 合盘/客观研究 | BaziCompatibilityAnalyzer、CaseObjectiveSummary等 | 双方冻结采用快照与个案信号；领域解释不伪装精密预测 |
| 截图/资料导入 | ImportRecognitionCoordinator、ScreenshotImportCommitter | 私有副本、确认上传/人工核对、可恢复会话；生产默认AI视觉，失败不静默转离线OCR |
| 外部AI与本地指令 | ExternalAnalysisBridge、BaziAiAnalysisPromptContract、AiCommentaryGenerator | 手动桥接/指令不联网；内置点评主动确认发送，结果预览/版本保存，不覆盖算法真值 |
| 单命例交换/完整备份 | SingleCaseExchange/BundleService、CaseBackupService | JSON引用与附件包区分；明文/密码、预演/显式计划/提交/journal恢复按DATA_CONTRACT |
| 云加密备份 | BaziCloudSyncCoordinator、Worker | 先启用/身份/恢复码确认，之后允许合并及周期任务；非空本机为源，空白设备解锁恢复 |
| 图像/文件交付 | CaseDetailLongImageCapture、MainActivity系统交付 | 真实页面渲染、恢复浏览现场；保存/分享结果与目标应用实际发送分开 |
| 设置/外观/诊断 | BaziSkin、AppFontSizePreference、AppDiagnostics、设置/关于页 | 现有入口、字体皮肤、密钥保护与脱敏诊断；不新增未经确认的常驻入口 |

## 输入、状态与完成定义

- 输入与规则显式化；不可确定的时间/来源保留不确定性，unsupported口径明确失败，不隐式降级。
- 用户按下排盘获得预览不等于持久化完成；保存、识别、恢复、分享各用自己的真实结果。
- 等待、成功、失败、取消、重复/冲突均需可解释，禁止通过清空状态、延时动画或隐藏错误假造流畅。
- 已恢复记录先显示，再静默校准；正常标签往返保留观察时刻与视口；新案例不能继承其他案例选择。
- 视觉缩小不削弱可用点击范围；折叠布局和悬浮导航尾部可达按架构/交互所有者处理。
- 外部能力发送范围可见，返回结果不自动写入，启用后的云后台行为见云合同。权限/配置存在不是服务已验收。

## 当前不承诺

在线精确地理编码/GPS、全部截图软件兼容、无需确认的自动OCR建档、私有问真数据库直接导入、多引擎裁决、确定性吉凶结论，以及未实施的平台。自动云备份不再属于“未实现/不承诺”；它只在启用并满足云合同后工作。

真实AI/OCR服务、Google/Supabase、跨设备恢复、全面目标设备交互、真实问真对账仍按PROJECT_STATUS缺口报告。真太阳时暂定应用规则不能因产品合同整理去掉PROVISIONAL。

## 变更与验收

功能范围变化先更新本合同及其领域/数据所有者，再按TEST_STRATEGY验证受影响入口；新增常驻按钮遵从用户功能审阅规则。只整理合同不新增功能、不重建冻结交付物。当前版本/包/构建配置读取app/build.gradle.kts；部署与覆盖安全以根AGENTS为准。
