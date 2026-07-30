# 当前交接：Stage 3B 单命例附件包第十七增量

更新日期：2026-07-30

## 当前结论

- Stage 0、Stage 1、Stage 2、Stage 3A 与 Stage 3B 当前实现均已完成并有自动化证据。
- Stage 3B 已覆盖单命例 JSON、单命例附件包、完整备份、明文/密码保护、非空库冲突
  计划、两阶段提交、附件事务及真实模拟器进程强杀恢复。
- 下一安全阶段是 Stage 4A：农历、地区/时区、真太阳时和基础排盘结果完整化。
- 问真输出仍是截图迁移的首要验收标准；算法真值仍由版本化规则和边界测试负责，二者
  不得混用。
- 未使用真实问真资料、真实姓名或用户截图；未安装或操作 OPPO，未 push、未发布。

## 本增量实现

### 兼容格式与用户入口

- 轻量单命例 JSON v1 继续固定为 `REFERENCES_ONLY`，保持既有交换兼容性；存在附件引用
  时仍禁止提交，避免产生断裂证据。
- 详情页“导出单命例”会在有附件时默认选择 `.nfbcase` 命例包，也允许用户明确改选
  只保存引用的 JSON。
- Android 通过 `CreateDocument`/`OpenDocument` 提供流；页面不解析 JSON、ZIP 或密码
  容器，也不申请宽泛存储权限。
- 明文导出继续显示敏感数据风险；命例包和 JSON 均可选择密码保护。

### `.nfbcase` 严格协议

- 命例包格式版本为 1，固定包含 `manifest.json`、`case.json` 与编号附件条目。
- manifest 绑定命例文档大小/SHA-256，以及每个附件的稳定 ID、ZIP 路径、字节数和
  SHA-256。
- 导出逐个读取 App 私有附件，大小和 SHA-256 不一致时拒绝，不输出伪完整包。
- 预览只在 App 私有临时区展开，限制条目数、单条目大小、总展开量、JSON 大小和安全
  相对路径；重复条目、路径穿越、未知版本、哈希或引用错误均零写入拒绝。
- 预览显示保护状态、聚合计数、附件校验结果与本地冲突，但不生成默认决策。
- 提交重新打开同一系统文件，重新核对 manifest、命例文档和冲突；来源变化或本地事实
  变化时拒绝沿用旧预览。

### 密码与附件事务

- 命例包密码容器使用独立 magic/type 和保护版本 1，采用
  PBKDF2-HMAC-SHA256 600,000 次 + AES-256-GCM；关键参数进入 AAD。
- ZIP 直接流式写入加密流，不整包进入内存；解密后的命例包仍执行全部 ZIP 与领域校验。
- 密码不写入文件、Room、日志或 ViewModel 持久状态；预览密码与提交密码分离，字符数组
  使用后清零。
- 保留两份会为命例、子项与全部附件生成新 ID，记录 `copiedFromCaseId`，并重写文本
  记录、历史、事件和字段证据中的附件引用。
- 范围合并只复制实际新增记录/历史/事件引用的来源附件；跨模块引用同一附件时只创建一份。
- 附件先进入 `.restore-staging/<transactionId>`，校验后切换到专属
  `restored/<transactionId>`；不会覆盖既有路径。
- 命例包与完整恢复共用有界恢复日志协议。数据库写入位于 Room 事务；失败只清理本事务
  新文件，启动恢复按提交前/后完整载荷和附件事实安全收尾。

### 共享边界

- `RestoreJournalProtocol` 统一日志模型、载荷哈希、目录常量和持久化入口；
  `CaseBackupService` 与 `SingleCaseBundleService` 不再维护两套恢复日志语义。
- `SingleCaseExchangeService` 继续拥有冲突、保留两份、模块去重和逐字段采用规则；
  附件包服务只负责容器、防御校验、附件重建和文件事务。
- `MainActivity` 按 `.json`、普通 `.nfbcase`、加密 `.nfbcase` 选择正确系统文档 MIME
  入口，并在提交时重开同一 URI。

## 所有者与边界

- 唯一计算入口：`BaziEngine.calculate()`。
- 唯一增量写入口：`CaseRepository`。
- 唯一单命例 JSON 入口：`SingleCaseExchangeService`。
- 唯一单命例附件包入口：`SingleCaseBundleService`。
- 唯一完整备份与恢复入口：`CaseBackupService`。
- 页面不直接访问 DAO、解析交换协议、操作附件事务或持久化密码。
- 仓库不得提交用户真实八字、姓名、截图、密钥或构建产物。

## 当前验证证据

全量命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test lint assembleDebug assembleRelease
```

- 95 条唯一单元契约；Debug/Release 变体合计 177 次执行，0 失败、0 跳过。
- 新增命例包服务测试覆盖：
  - 明文导出与严格零写入预览；
  - 加密包无密码/错密码/正确密码；
  - 保留两份后全部附件身份与引用重映射；
  - 范围合并跨记录/事件附件去重；
  - 强制数据库失败时 Room 回滚、本事务文件清理与随后重试成功。
- App 与 `core:data` Lint 均 0 错误；仅有 9 + 5 条依赖版本提示。
- Debug 与未签名 Release 均构建成功。
- API 35 模拟器 `ExpenseCapture_API35`：
  - 原 Stage 2/3 主流程 1/1 通过，覆盖 JSON、密码 JSON、完整备份和恢复；
  - 新增命例包系统文件流程 2/2 通过：明文与密码 `.nfbcase` 均真实经过
    DocumentsUI；密码包在预览和提交分别认证，最终从 App 私有目录读回相同附件字节、
    SHA-256 和重映射后的记录引用；
  - 真实进程强杀恢复再次通过：强杀 PID 3348 后由新进程冷启动，日志、暂存和最终孤儿
    目录全部清除；
  - 全部设备测试仅在 `emulator-5554` 执行，未触碰 OPPO。
- 真实问真迁移仍未执行；自动化证据不能替代最终隐私批准样本验收。

## APK

Debug 验收构建：

`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha22-debug.apk`

- 大小：9,967,338 bytes
- SHA-256：`dfea1249705a36a2f3017854197c5acdf8ada429c443c208d4d469faa577c9f7`

未签名 Release：

`app/build/outputs/apk/release/NanfengBazi-Android-v0.3.0-alpha22-release-unsigned.apk`

- 大小：6,801,329 bytes
- SHA-256：`f0617fe9b81883f4a46281dffac704172236272251e6adfd5429449b3aa58cb0`

## 当前限制与风险

- 编辑器仍只支持公历民用时；农历、地区、DST 选择、真太阳时与更多基础盘字段未接通。
- 问真截图导入、离线 OCR、页面分类、多图归组与可恢复导入会话尚未实现。
- 软删除没有永久清理入口，这是数据安全选择；正式清理仍需用户可验证备份和附件引用计数。
- App 仍是手机单列工作台，OPPO Find N5 展开双栏、无障碍和大字体尚未验收。
- 当前 Debug APK 不是正式签名 Release；OPPO 数据保留安装与发布需要用户明确授权。

## 下一安全增量

进入 Stage 4A，优先顺序：

1. 固化农历/闰月输入与公农历双向转换证据；
2. 接通地区、经纬度、时区/UTC offset 与 DST 歧义确认；
3. 实现独立版本化真太阳时组件，覆盖跨日和跨时辰边界；
4. 扩展基础排盘结果：生肖、节气、十神、藏干、纳音、长生、空亡等；
5. 更新详情展示与黄金边界集，再进入 Stage 4B 的 50+ 样本门禁。

`docs/REQUIREMENT_GAP_AUDIT.md` 是 v1.0 的逐项事实清单；Stage 3B 完成不等于整个
产品已经落地。
