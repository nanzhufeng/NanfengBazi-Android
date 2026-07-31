# 下一轮 Codex 启动提示

把下面整段复制到新的 Codex 对话中：

```text
你现在接手“南枫八字”Android App 的后续开发。

仓库：
/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi

先只读执行以下动作：
1. 确认 Git 根目录、当前分支和工作区状态。
2. 完整读取仓库 AGENTS.md。
3. 读取 docs/CURRENT_HANDOFF.md。
4. 只读取本次任务相关的 docs/REQUIREMENT_GAP_AUDIT.md、
   docs/architecture-governance.md、docs/domain-rules.md、docs/TEST_STRATEGY.md
   和 docs/decision-log.md 片段。
5. 使用 developing-apps-with-governance 和
   develop-apps-with-nanfeng-product-standards Skill。

现场代码和最新测试证据优先于交接文档；若不一致，先报告并修正文档。
不要读取旧对话全文，不要把其他 App 的事实带进本项目。

当前唯一任务：实现 VX-10 客观命盘摘要。

产品边界：
- 问真截图来源值与本机算法真值继续分开。
- 摘要只消费当前命例唯一已采用快照中的确定性字段和版本证据，不重新计算第二套结果。
- 旧快照缺字段时明确显示缺失，不根据四柱反推补造。
- 问真来源字段、人工点评和命主反馈只可作为明确标注的已有记录计数。
- 不生成吉凶、合婚、关系、事业、婚姻、健康或其他确定性人生结论。

实现要求：
1. 在 core:domain 定义版本化摘要输入、章节、字段、来源、结构化失败和公开接口。
2. 首版覆盖输入历法、四柱、日主、基础盘、起运大运和计算档案等已版本化事实。
3. UI 只展示和复制领域摘要，不直接调用 Tyme4j，也不形成第二算法真值。
4. 对所有缺失、旧版本、无采用快照和多采用快照状态给出可解释结果。
5. 增加禁止性文案扫描，防止引入吉凶和确定性人生结论。
6. API 35 只使用 emulator-5554 完成页面打开、复制摘要和状态恢复。
7. 更新受影响的需求审计、架构所有权、领域规则、测试策略、决策日志和交接文档。
8. 升级到下一 alpha，冻结 Debug 与未签名 Release 的大小和 SHA-256。

安全边界：
- 不操作 OPPO 或其他真机，不卸载、不清数据。
- 不联网、不接外部 AI、不发送命例资料。
- 不提交真实姓名、八字、截图、密钥或构建产物。
- 不 push、不发布；完成后只做本地准确提交。
- 保护任何现场未提交改动；若发现无关或无法安全拆分的改动，停止并说明。

最低验证：
- 定向领域摘要、ViewModel、禁止性文案扫描和系统入口测试。
- git diff --check。
- clean test lint assembleDebug assembleRelease assembleDebugAndroidTest。
- emulator-5554 专项流程。
- 分开报告实现、测试、构建、模拟器和未验证外部门禁。

完成客观命盘摘要增量后先收口、提交并更新交接，不自动触碰真实问真、外部 AI、
OPPO、签名或发布门禁。
```
