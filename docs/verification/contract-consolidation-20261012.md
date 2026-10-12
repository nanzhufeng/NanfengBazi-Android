# 合同统一与冲突处理 · 2026-10-12

## 范围和基线

- 生产实现基线e48996b6071f0c2f6f4dad99331eed475544e69c；本轮开始时已有13项上一轮文档/Skill增量及一项无关删除，均按归属保留。未改业务代码、测试、依赖、构建配置或设备数据。
- 全部项目合同入口、根AGENTS、五个项目Skill、架构/需求/领域/数据/云/测试/样本/第三方/图标、状态/经验/决策与历史报告均纳入分类。扫描不等于所有业务行为已验收。
- 唯一现行路由为[CONTRACT_INDEX](../CONTRACT_INDEX.md)。历史阶段全文完整归档，历史决策/性能/经验报告保留原时间和适用范围，不能被当当前授权或运行指令。

## 冲突与处理清单

| 编号 | 旧冲突/维护风险 | 当前依据 | 统一处理 |
| --- | --- | --- | --- |
| C01 | 需求把流月/日/时、反查、摘要/导出等列作未来增强 | ProfessionalFortuneResolver、FourPillarsLookup、ObjectiveSummary、长图实现 | 现行产品表列已有入口，原规划归档 |
| C02 | 需求排除自动云同步、WorkManager只用于本地解析 | BaziCloudSyncCoordinator/Work、AppContainer失效监听与启用状态 | 云合同拥有启用后后台、本机源和空白恢复语义 |
| C03 | 默认端侧离线OCR约定与生产注入不一致 | AppContainer注入AiVisionScreenshotOcrEngine；MLKit仍有实现/依赖 | 明确生产AI视觉、上传确认、失败不静默降级 |
| C04 | 差距审计称合并Manifest无联网权限 | app/src/main/AndroidManifest.xml明确INTERNET | 区分核心离线与可选联网；旧alpha75审计归档 |
| C05 | 差距审计称永久物理清理不开放 | StageTwoViewModel/RoomCaseRepository永久删除入口及回收站边界 | 当前产品明确区分软删除、恢复、永久删除，不恢复旧功能禁令 |
| C06 | 架构消费者仍写未来仓储/未来OCR | AppContainer、CaseRepository、Committer当前调用 | 以当前所有者矩阵取代多份阶段入口表 |
| C07 | “所有计算不得直接调用Tyme”扩大成禁用合法历法API | AlmanacReader/选择器转换/反查、BaziEngine分工 | 出生四柱排盘单入口，既有历法用途明确分层 |
| C08 | 测试要求每次计算恢复八字全局provider | TymeEightCharResolver正向显式provider；TymeFourPillarsLookup受锁finally例外 | 区分正向不修改与反查临时全局作用域；起运provider仍受保护 |
| C09 | 第三方说明前八步大运、lunar仅未来候选 | TymeBaziEngine/专业大运覆盖与engine-tyme test依赖lunar1.7.7 | 更新十二步/120年和测试对照身份，不成为生产第二引擎 |
| C10 | 旧测试允许OPPO自动启动或只避免“同时触碰” | 当前用户永久禁止connected任务及主设备Debug/自动启动等 | 现行测试只路由根安全合同，历史脚本不授权 |
| C11 | 旧门禁固定每轮构建测试APK/所有包 | 当前最小充分验证与冻结产物合同 | 按改动风险选择门禁，文档不重建冻结APK |
| C12 | domain与DATA同时拥有附件/密码/计划/恢复细则 | CaseBackupService、Exchange/Bundle、journal测试 | 详细安全细则迁入DATA唯一正文，domain仅链接 |
| C13 | 手动AI桥接的“App不联网”容易作用于整App | ExternalAnalysisBridge与Generator/视觉/云不同链路 | 限定“此本地指令/手动桥接不联网”，不扩大结论 |
| C14 | alpha/Stage完成表、决策与当前规则混写 | 现行源码与适用报告、已有历史日期 | 当前需求/架构/测试/缺口重整，阶段全文原字节归档，决策标历史职责 |
| C15 | 版本/Schema/恢复能力等冲突上一轮已纠正 | 当前DATA与复盘报告 | 保留已完成修正，不重复业务修复或重跑旧门禁 |

## 不修改的事项与剩余边界

- archivesName落后versionName、动态APP_BUILD_TIME与大型app文件是已知配置/维护风险；本轮只统一合同，不改现有功能或构建行为。
- 真太阳时PROVISIONAL、真实问真包对账、真实AI/Google/Supabase、跨设备恢复与全面主设备性能仍缺对应证据，不能写成全部验收。
- 合同正确性以可定位代码/验证复核，不借“最新实现”降低用户安全规则；真实服务、设备或正式发布没有在本轮执行。
- 既有docs/next-codex-prompt.md删除保持未恢复/未暂存；不重写原验证报告或复盘清单中的历史事实。

## 验证与追溯

本轮合同检查、相对链接/Skill/JSON、生产文件哈希与diff校验结果在同目录contract-consolidation-20261012.json记录。业务JVM/Lint沿用2026-10-11既有报告，本轮未重跑、不刷新其执行时间。检查器覆盖机械漂移，不证明语义完全无遗漏。

精确回滚快照在本轮本地备份；永久历史原文位于docs/history/contracts，各原文字节SHA-256在机器报告登记。后续变更只维护主题所有者，再运行scripts/check_contract_consistency.py并同步适用验证。

## Checkpoint复查增量 · 2026-10-12

- C16：脚本ACTIVE与合同索引需要双重维护，新增主题可能漏检。现由索引“类型”列直接确定现行/审计范围，历史/接续不作为规范执行；未知类型、必需所有者或文件缺失均拒绝。
- C17：存在文件但章节已改名仍会断链。增加本地Markdown锚点校验，包括中文标题、同文件引用和重复标题；拒绝链接逃逸checkout。
- C18：单行./gradlew匹配会漏掉无./、模块前缀与多行命令。增加命令块/内联命令检查，危险connected任务由回归反例覆盖。
- C19：Skill未出现在glob或UI字段为空仍可能PASS。索引明确登记五个必需Skill，文件与非空UI元数据纳入检查。

新增scripts/test_check_contract_consistency.py的9项回归通过；既有性能脚本4项隔离测试通过。现行范围检查18个文件/Skill、47个链接通过；业务JVM/Lint未重跑，原13:08证据不刷新。最终本地checkpoint纳入本轮归属文档/Skill/脚本与此前复盘新增清单，无关删除排除。

归档需求原文第3–7行保留原有Markdown双空格换行；为保持逐字节历史证据，未删空格。现行文件按默认diff --check检查，归档只对该尾随空格规则作精确范围例外，同时核验原文字节SHA-256。
