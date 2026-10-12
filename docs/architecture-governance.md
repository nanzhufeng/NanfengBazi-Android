# 现行架构与治理合同

> 所有权正文。规则与证据职责见[合同索引](CONTRACT_INDEX.md)，各阶段入口矩阵和旧数值完整保留于[历史架构](history/contracts/architecture-governance-before-20261012.md)，不再作为平行实现要求。

## 模块与依赖

AppContainer手工组装六个Gradle模块：app消费domain/data/engine-tyme/image-parser；engine-tyme消费domain/solar-time；solar-time、data、image-parser消费domain。domain不依赖Android页面。MainActivity处理系统入口，StageTwoViewModel编排主要状态，Compose呈现。实际依赖以settings/build文件为准，不为历史规划引入新框架。

## 唯一所有者

| 概念 | 所有者/生产实现 | 例外和禁止事项 |
| --- | --- | --- |
| 出生输入、计算配置/快照 | core/domain模型、CaseWorkflow/既有用例 | 结构化输入/版本，来源与采用保真，不只保存展示字符串 |
| 正向出生排盘 | BaziEngine → TymeBaziEngine | 不由UI/导入/数据库复制四柱算法 |
| 专业岁运、当前定位 | ProfessionalFortuneResolver / FortunePositionResolver → Tyme实现 | 领域层拥有父区间、年龄、藏干、观察时刻；UI不另算 |
| 农历/万年历/反查 | 既有选择器转换、AlmanacReader、FourPillarsLookup | 不是第二套出生排盘；反查候选仍须正向复核。不得把“禁止复制算法”扩大为所有Tyme历法类型一概禁用 |
| 时区/真太阳时 | BirthTimeZoneResolver、TrueSolarTimeCalculator | 原始墙上时间和offset证据保留，PROVISIONAL见domain-rules |
| Tyme全局provider | engine-tyme反查受锁作用域 | 正向使用显式provider而不改全局；反查finally恢复全局，不要求每次正向计算无意义重置 |
| 增量聚合写入 | CaseRepository → RoomCaseRepository | expectedRevision/事务/三态/顺序；UI/解析器不直写DAO |
| 特权恢复/交换 | CaseBackupService、SingleCaseExchange/BundleService | 数据层拥有批量事务和附件/journal；与普通增量写入区分，不另建页面恢复管线 |
| Schema与迁移 | NanfengBaziDatabase/DatabaseMigrations、已提交schemas | 禁止破坏性迁移；准确版本见DATA_CONTRACT与源码 |
| 目录/分组投影 | CaseCatalogStore、CaseListSession、ViewModel | 缓存可丢弃，Room是真值；旧会话不覆盖当前页 |
| 详情/笔记/事件/草稿 | caseId+revision聚合、StageTwoViewModel与CaseManagement用例 | 缓存未知不作空数据；更新/冲突保留编辑输入 |
| 名人成品资料/案例库 | UnifiedCelebrityCatalogImporter → CaseRepository、libraryType | 资料包只作输入；系统库类型不由分组/标签或页面临时猜改 |
| 合盘/派生研究 | domain分析器、采用快照与本地合盘历史Store | 内容必须来自真实信号，不以固定模板伪装个案结论 |
| 来源图片/导入会话 | PrivateImportImageStore、ImportSessionRepository | 系统URI及时私有复制，后台不持有外部URI/Bitmap作为长期真值 |
| 生产截图识别 | AiVisionScreenshotOcrEngine → ImportRecognitionCoordinator → parser/refiner | 先确认上传/核对，失败不静默回退；MLKit实现存在不是生产默认离线证明 |
| 截图提交 | ScreenshotImportCommitter → CaseRepository | 来源/计算/采用分开，不跳过字段/重复候选确认 |
| 手动外部桥接/本地AI指令 | ExternalAnalysisBridge、BaziAiAnalysisPromptContract | 此路径不联网；不能推广为整个App无联网能力 |
| 内置AI点评 | 既有PromptContract、Generator、CaseNotesEditor/TextRecord用例 | 主动发送、Keystore加密、预览/版本保存，不自动覆盖其他记录 |
| 云结构化备份 | BaziCloudSyncCoordinator/Bridge、CaseBackupService、Worker | 启用后的后台语义见cloud-sync-contract；不复制页面/Worker同步真值 |
| 页面长图与系统交付 | CaseDetailLongImageCapture、MainActivity系统文件入口 | 同真实Compose渲染器、finally恢复浏览；不另画平行报告 |
| 主题/皮肤/字体/五行/触觉 | NanfengBaziTheme、BaziSkin、AppFontSizePreference、BaziElementPresentation、AppHapticFeedback | 表面/点击/反馈同形；不在各页复制识别/配色 |
| 脱敏诊断 | AppDiagnostics及结构化状态 | 不输出身份/来源内容/凭据/原异常；分享不等于外部业务完成 |

## 状态和高频交互

1. 详情由完整案例聚合原子发布，绑定caseId+revision。已采用快照与子状态同行；校准保留正确首帧，过期协程不能覆盖离开/新案例。
2. 记录目录先恢复内存/磁盘投影，再做维护和静默校准。发布锁与写盘锁分离，异步持久化有代次守卫；局部置顶/软删除等优先精确回写，同步工具栏/计数/分组/行会话。
3. 标签切换只改呈现；已完成专业观察时刻/结果与saveable轴状态按案例保留。新案例建立自己的观察seed，用户显式“今天”或日期选择重新定位。
4. 专业内容按视口组装，短行与长惰性行沿同一列宽合同。选择由解析器成功后原子采用，最新请求守卫避免连续点击旧结果回写。具体领域区间见domain-rules。
5. 导出临时展开现有页面，不复制算法或视觉报告；成功/失败/取消均恢复标签/模式/轴状态，不能把导出布局常驻到正常浏览。
6. 内外屏/宽窄布局使用明确分支与参数；insets只计一次，悬浮导航尾部保持可达。缩小视觉/预览仍保留命中区域，表面/clip/ripple/pressed/focus同形。
7. 选器连续效果在graphicsLayer/derivedStateOf读取；已有正确位置不重复滚动。设置分支返回保留根列表状态，皮肤仅采纳settledPage，不在拖动中重建全局主题。
8. 长库解析/计算/持久化按合适dispatcher执行；取消原样传播，未完成状态不得静默冒充成功。性能指标按TEST_STRATEGY分层。

## 跨层变更门槛

一个概念由其唯一所有者给出结果，其他入口只消费；重排UI不能复制ViewModel、用例、数据库、备份或同步。Schema/算法/格式变更必须同步版本、兼容路径和行为测试；新配置/索引也属于验证范围。当前能力不以“未来仓储/未来OCR”描述，动态版本、测试数和远端状态只放PROJECT_STATUS。
