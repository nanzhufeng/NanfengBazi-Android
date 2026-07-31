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

当前唯一任务：实现 VX-05 命主反馈主题标签候选增强。

产品边界：
- 问真截图来源值与本机算法真值继续分开。
- 完整 `OWNER_FEEDBACK` 原文、既有事件与版本历史继续是来源事实，不得截断、重排或
  覆盖。
- 首版只用确定性、本地规则提取可解释、可编辑、可逐条确认的主题标签候选。
- 候选不代表用户已经确认，也不是本机算法真值；未经确认不得写入正式标签或分类。
- 不联网、不调用外部 AI，不自动生成新的命理断语。

实现要求：
1. 在 core:domain 定义版本化反馈主题候选、来源定位、规范标签、规则证据、审核状态、
   结构化失败和公开提取接口。
2. 确定性解析适配放在合适的输入/解析层；页面不得内置第二套关键词或分句规则。
3. 候选必须保留可解释来源证据，相同来源重复提取结果稳定并去重。
4. UI 支持逐条采用、编辑和拒绝；确认后进入既有正式标签/分类边界，原反馈与事件不变。
5. 覆盖错误来源、空反馈、无候选、多候选、重复标签、来源定位和 revision 过期。
6. API 35 只使用 emulator-5554 完成反馈—候选—确认—正式标签/分类—状态恢复流程。
7. 更新受影响的需求审计、架构所有权、领域规则、测试策略、决策日志和交接文档。
8. 升级到下一 alpha，冻结 Debug 与未签名 Release 的大小和 SHA-256。

安全边界：
- 不操作 OPPO 或其他真机，不卸载、不清数据。
- 不联网、不接外部 AI、不发送命例资料。
- 不提交真实姓名、八字、截图、密钥或构建产物。
- 不 push、不发布；完成后只做本地准确提交。
- 保护任何现场未提交改动；若发现无关或无法安全拆分的改动，停止并说明。

最低验证：
- 定向领域提取、来源证据、ViewModel、正式写入和系统入口测试。
- git diff --check。
- clean test lint assembleDebug assembleRelease assembleDebugAndroidTest。
- emulator-5554 专项流程。
- 分开报告实现、测试、构建、模拟器和未验证外部门禁。

完成命主反馈主题标签候选增量后先收口、提交并更新交接，不自动触碰真实问真、外部 AI、
OPPO、签名或发布门禁。
```
