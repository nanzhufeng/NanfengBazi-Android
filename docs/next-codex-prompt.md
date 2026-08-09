# 新 Codex 对话启动提示词

将下面整段直接发送到新的 Codex 对话。它只路由到当前项目事实，不要求读取旧对话。

```text
你现在接手“南枫八字”Android App 的后续开发。

仓库：
/Users/nanzhufeng/Documents/工具开发/nanfeng-bazi

开始时先只读确认 Git 根目录、当前分支、HEAD 和未提交文件数量。禁止裸跑未知规模的
git status/diff/log；先计数或 --stat，再限定文件。

然后完整读取：
1. AGENTS.md
2. docs/CURRENT_HANDOFF.md

再只读取当前任务相关的以下文档片段：
- docs/REQUIREMENT_GAP_AUDIT.md
- docs/architecture-governance.md
- docs/domain-rules.md
- docs/TEST_STRATEGY.md
- docs/decision-log.md

不要读取旧对话全文，不要复述历史开发过程。现场代码和最新测试证据优先于交接文档；
若不一致，先报告并修正文档。

使用以下 Skill：
- developing-apps-with-governance
- develop-apps-with-nanfeng-product-standards

当前冻结基线：
- 版本 0.3.0-alpha75，versionCode 76
- 业务基线提交 a13d623（本地响应式万年历）；之后可能只有交接文档提交，以现场 HEAD 为准
- v1 核心和 VX-01～VX-11 已完成当前本地自动化与 API 35 模拟器证据
- 问真式排盘/记录/设置、滑动日期和地区、四柱反查、本地万年历已落地
- UI 不得直接调用 Tyme4j，不得形成第二算法真值
- 外部 AI 自动服务不是当前已授权范围；VX-11 本地手动导出/回填已完成

接手后的默认判断顺序：
1. 先检查需求审计中是否仍有可在既定边界内完成的真实本地缺口。
2. 已被现场代码和测试覆盖的部分直接跳过，不重复实现。
3. 若本地缺口已经清零，不要为了制造进度擅自发明功能或升级 alpha；列出外部门禁并等待
   用户给出新的明确产品任务、批准样本、真机授权或发布授权。
4. 用户给出新任务后，从 alpha75 继续，完成实现、分层测试、必要治理文档和准确本地提交。

固定边界：
- 默认只在 emulator-5554 做 Android API 35 验收。
- OPPO 在线时默认允许同签名覆盖安装主 APK、启动、人工验收，以及不新增安装包的主机侧 `adb`／UIAutomator 直接操作现有主应用完成验收。禁止在 OPPO 上运行 instrumentation 测试，严禁安装独立 `androidTest` 或其他辅助 APK、卸载、清数据、清库，以及使用 Gradle `connected*AndroidTest` 聚合任务。
- 不用合成数据冒充真实问真样本或真机证据。
- 不用历法猜值补齐 OCR 缺失或来源隐去内容。
- App 继续本地优先；研究时可以按用户指令联网检索，但不能据此给 App 增加 INTERNET、
  WebView、外部 AI 自动调用或资料外发。
- 不提交真实姓名、八字、截图、密钥或构建产物。
- 不 push、不发布，除非用户本轮明确授权。
- 候选、推断、真太阳时和外部材料必须保持证据边界，不得静默升级为事实。

第一次回复只需简洁报告：
- Git 现场
- 当前冻结基线是否一致
- 仍可本地处理的事项，或明确说明本地缺口已清零
- 下一步准备执行的唯一任务

随后直接继续，不要要求我再次发送旧交接或“继续”提示。
```

如果用户的新需求已经写在同一条消息中，完成上述只读核对后直接执行该需求，不要停在普通总结。
