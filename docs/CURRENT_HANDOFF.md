# 当前交接：Stage 3B 安全导入第三增量

更新日期：2026-07-30

## 当前结论

- Stage 0、Stage 1、Stage 2 与 Stage 3A 已有当前实现和证据，Stage 3B 正在进行。
- 本增量完成无附件单命例的“跳过/保留两份”决策、提交前冲突复查和原子写入。
- v1.0 总方案仍未完成；模块/字段合并、附件事务、密码加密和进程恢复仍待落地。
- 未使用真实问真资料、真实姓名或用户截图；未安装或操作 OPPO 真机，未 push、未发布。

## 本增量实现

- 详情页新增“导出单命例 JSON”，先确认未加密敏感数据风险和附件仅引用限制；
- `CreateDocument(application/json)` 使用规范文件名创建用户可访问文件；
- 列表页新增“导入预览”，通过 `OpenDocument` 读取 JSON，不申请宽泛存储权限；
- ViewModel 在 IO 调度器打开/关闭流，只调用 `SingleCaseExchangeService`，页面不解析协议；
- 预览弹窗显示来源 App/Schema/时间、记录/事件/历史/附件计数及冲突候选理由；
- 预览是第一阶段零写入；提交前重新查询冲突，预览过期则拒绝并要求重新预览；
- “保留两份”重建命例、记录、历史、事件、快照、分组和标签 ID，并记录来源命例 ID；
- “跳过”明确零写入；带附件引用的 JSON 因没有图片二进制而禁用提交；
- 错误弹窗保留协议错误代码，导出成功明确图片仅保留引用；
- 启用 App `BuildConfig.VERSION_NAME`，导出文件记录真实当前版本；
- Debug 版本升级到 `0.3.0-alpha08`。

## 所有者与边界

- 唯一计算入口：`BaziEngine.calculate()`。
- 唯一增量写入口：`CaseRepository`。
- 唯一完整恢复入口：`CaseBackupService`。
- 唯一单命例交换入口：`SingleCaseExchangeService`；只有第二阶段提交允许调用保存。
- 命例生命周期编排：`CaseLifecycleUseCase`，最终仍通过 `CaseRepository.save()`。
- 重复候选事实：`CaseRepository.findDuplicateCandidates()`。
- 页面不直接访问 DAO，不自行删除、复制或判断重复。
- 页面不生成历史快照；`TextRecordUseCase`、`CaseEventUseCase` 通过聚合写入口统一追加。
- 永久物理清理必须先补齐备份、附件引用计数、失败恢复和明确确认边界。

## 当前验证证据

自动化命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug lintDebug --warning-mode all
```

- 本地自动化：70 条唯一单元契约；Debug/Release 变体合计 127 次执行，0 失败；
- 覆盖领域、引擎、Room v5 往返、v1→v5 迁移、旧 Schema v2 备份恢复、历史基线补建、
  删除保留历史、单命例往返/哈希/版本/大小/明文确认/冲突零写入、生命周期用例、
  重复确认、ViewModel 和导航；
- `assembleDebug`：成功；
- `lintDebug`：成功，0 错误；App 8 条、数据模块 5 条依赖版本提示；
- API 35 模拟器 `ExpenseCapture_API35`：
  - Compose 自动化 1/1 通过，新增/修改分类分析并读取旧版本；
  - 同一流程还完成重复提示与确认→编辑→新增带标题/事业类别事件→详情及历史读回
    →Android 创建文档并保存 JSON→复制→移入回收站→检索→恢复→Android 打开同一
    JSON→稳定 ID 冲突预览→保留两份提交→列表读回第二份；
  - Compose + UiAutomator 1/1 通过，真实经过系统 DocumentsUI，不是内存流替代；
- OPPO 设备虽然连接，但未安装、未操作；以上证据不能替代真机验收；
- 真实问真迁移：未执行。

## APK

`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha08-debug.apk`

大小：9,699,794 bytes

SHA-256：`1166da15ab8ed5a0e9071b740903fb04ead1154854582e67f5eabacdbab205ab`

该 APK 是 Debug 验收构建，不是正式签名 Release。

## 当前限制与风险

- 软删除没有永久清理入口，这是数据安全选择，不是静默遗漏；
- 副本有意不复制记录和附件，完成反馈已明确范围；
- 编辑器仍只支持公历民用时；农历、地区、时区确认和真太阳时未接通；
- 单命例只实现“跳过/保留两份”；按模块合并和逐字段采用尚未实现；
- `REFERENCES_ONLY` 不携带附件二进制，当前带附件引用命例会拒绝提交；
- 加密备份、附件事务、完整备份非空库两阶段提交和进程强杀恢复仍未实现；
- 没有进程重启、OPPO Find N5、真实 ZIP 或真实问真样本证据。

## 下一安全增量

继续 Stage 3B：

1. 定义按模块合并、逐字段采用的差异与采用值模型；
2. 补齐合并提交的修订保护、历史追加和失败回滚；
3. 再补密码加密、附件事务和进程重启恢复日志。

`docs/REQUIREMENT_GAP_AUDIT.md` 是 v1.0 的逐项事实清单；任何单阶段完成都不能
表述为全项目落地。
