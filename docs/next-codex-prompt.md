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

当前唯一任务：实现 VX-09 四柱反查。

产品边界：
- 问真截图迁移保真与本机算法真值分开。
- 唯一正向计算入口是 BaziEngine.calculate()，UI 不得直接调用 Tyme4j。
- 四柱反查首版输入四柱、起止年份、IANA 时区和子时口径，返回可解释且可复算的
  民用时候选；不静默推算真太阳时。
- 候选不是出生分钟的唯一证明，页面必须明确说明。
- 不生成吉凶、合婚、关系、事业、婚姻或健康结论。

实现要求：
1. 在 core:domain 定义查询、候选、结构化错误和公开接口。
2. 在 core:engine-tyme 实现适配器；可调查 Tyme4j 1.5.1
   EightChar#getSolarTimes(int,int)，但必须隔离并恢复全局 LunarHour.provider。
3. 每个候选必须用项目规则复核四柱，不能直接信任库返回列表。
4. app 负责用例协调、Compose 页面和 SavedStateHandle；不得形成第二算法入口。
5. 覆盖有效/无解/非法干支/范围上限/跨 60 年周期/两种子时口径/
   provider 异常恢复。
6. API 35 只使用 emulator-5554 完成真实输入—查询—结果—状态重建流程。
7. 更新受影响的需求审计、架构所有权、领域规则、测试策略、决策日志和交接文档。
8. 按版本顺序升级到下一 alpha，冻结 Debug 与未签名 Release 的大小和 SHA-256。

安全边界：
- 不操作 OPPO 或其他真机，不卸载、不清数据。
- 不联网、不接外部 AI、不发送出生资料。
- 不提交真实八字、姓名、截图、密钥、构建产物。
- 不 push、不发布；完成后只做本地准确提交。
- 保护任何现场未提交改动；若发现无关或无法安全拆分的改动，停止并说明。

最低验证：
- 定向领域/引擎/ViewModel 测试。
- git diff --check。
- clean test lint assembleDebug assembleRelease assembleDebugAndroidTest。
- 模拟器专项流程。
- 分开报告实现、测试、构建、模拟器和未验证外部门禁。

完成四柱反查增量后先收口、提交并更新交接，不自动触碰真实问真、外部 AI、
OPPO、签名或发布门禁。
```
