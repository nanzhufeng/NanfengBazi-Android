# 当前交接：Stage 3A 命例生命周期第三增量

更新日期：2026-07-30

## 当前结论

- Stage 0、Stage 1、Stage 2 与 Stage 3A 三个受控增量已有当前实现和证据。
- 本增量完成可恢复软删除、回收站、恢复、研究副本和重复候选提示。
- v1.0 总方案仍未完成；Stage 3A 尚缺记录级历史和统一分析分区。
- 未使用真实问真资料、真实姓名或用户截图；未安装或操作 OPPO 真机，未 push、未发布。

## 本增量实现

- `BaziCase` 与 `CaseSummary` 保存 `deletedAt` 和 `copiedFromCaseId`；
- Room 升级到 Schema v4，提交 v1→v2→v3→v4 迁移证据与 Schema JSON；
- 活动列表、回收站和全部命例由 `CaseVisibility` 明确区分；
- 整例删除只写入 `deletedAt`，记录、事件、计算快照、来源证据和附件引用完整保留；
- 回收站命例为只读，不更新最近查看；恢复继续使用原稳定 ID 和全部历史；
- 当前不提供自动过期、清空回收站或物理删除入口；
- 命例复制生成新命例 ID、新快照 ID 和 `copiedFromCaseId`；
- 复制保留出生资料、分组、标签和计算快照，不复制记录、事件、来源附件或字段证据；
- `CaseRepository.findDuplicateCandidates()` 统一按出生时间加性别、已采用四柱给出理由；
- 新建和编辑在第一次保存前显示活动/回收站候选，确认前不写数据库；
- 用户确认后允许保留两份，不自动合并、覆盖或恢复旧命例；
- Debug 版本升级到 `0.3.0-alpha03`。

## 所有者与边界

- 唯一计算入口：`BaziEngine.calculate()`。
- 唯一增量写入口：`CaseRepository`。
- 唯一完整恢复入口：`CaseBackupService`。
- 命例生命周期编排：`CaseLifecycleUseCase`，最终仍通过 `CaseRepository.save()`。
- 重复候选事实：`CaseRepository.findDuplicateCandidates()`。
- 页面不直接访问 DAO，不自行删除、复制或判断重复。
- 永久物理清理必须先补齐备份、附件引用计数、失败恢复和明确确认边界。

## 当前验证证据

自动化命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug lintDebug --warning-mode all
```

- 本地自动化：56 条唯一契约；Debug/Release 变体合计 101 次执行，0 失败；
- 覆盖领域、引擎、Room 往返、v1→v4 迁移、Schema v2 备份恢复、生命周期用例、
  重复确认、ViewModel 和导航；
- `assembleDebug`：成功；
- `lintDebug`：成功，0 错误；App 8 条、数据模块 5 条依赖版本提示；
- API 35 模拟器 `ExpenseCapture_API35`：
  - Compose 自动化 1/1 通过；
  - 在已有持久测试数据下完成重复提示与确认→编辑→复制→移入回收站→检索→恢复；
- OPPO 设备虽然连接，但未安装、未操作；以上证据不能替代真机验收；
- 真实问真迁移：未执行。

## APK

`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha03-debug.apk`

大小：9,805,902 bytes

SHA-256：`8270ce02309e9ac3433aded96cf851ac5c181977d085657ebf4a856decb002c1`

该 APK 是 Debug 验收构建，不是正式签名 Release。

## 当前限制与风险

- 文本记录和事件没有记录级版本历史，分析分类仍是通用模型；
- 软删除没有永久清理入口，这是数据安全选择，不是静默遗漏；
- 副本有意不复制记录和附件，完成反馈已明确范围；
- 编辑器仍只支持公历民用时；农历、地区、时区确认和真太阳时未接通；
- 单命例 JSON、加密备份、非空库冲突预览和两阶段提交尚未实现；
- 没有进程重启、OPPO Find N5、真实 ZIP 或真实问真样本证据。

## 下一安全增量

继续 Stage 3A：

1. 为文本记录、命主反馈、师傅点评、分析和关键事件建立记录级版本历史；
2. 建立事业、财运、婚姻、健康、学业、家庭、其他的统一分析分区；
3. 保持来源附件、原文、修改前值、采用值和顺序可追溯；
4. 完成后再进入 Stage 3B 的单命例 JSON、加密 ZIP 和非空库冲突预览。

`docs/REQUIREMENT_GAP_AUDIT.md` 是 v1.0 的逐项事实清单；任何单阶段完成都不能
表述为全项目落地。
