# 当前交接：Stage 3A 记录历史第四增量

更新日期：2026-07-30

## 当前结论

- Stage 0、Stage 1、Stage 2 与 Stage 3A 四个受控增量已有当前实现和证据。
- 本增量完成文本记录、分析和关键事件的不可覆盖版本历史，以及统一分析分类。
- v1.0 总方案仍未完成；关键事件的结构化标题/分类仍需补齐后再结束 Stage 3A。
- 未使用真实问真资料、真实姓名或用户截图；未安装或操作 OPPO 真机，未 push、未发布。

## 本增量实现

- `BaziCase` 新增 `textRecordRevisions` 和 `eventRevisions`，每条历史保存稳定身份、
  版本号、变更类型、完整快照和变更时间；
- 新增、修改、删除分别追加 `CREATED`、`UPDATED`、`DELETED`，不覆盖旧版本；
- 旧库条目没有历史时，首次修改或删除会先把原值补建为版本 1；
- 删除当前记录或事件只移除当前列表项，不删除历史；整例软删除与恢复继续保留全部历史；
- 分析记录统一使用综合、性格、事业、财运、感情、健康、学业、家庭、其他九类；
- 非分析记录不能保存分析分类；旧分析缺失分类时 UI 按“综合”显示；
- Room 升级到 Schema v5，提交 v1→v2→v3→v4→v5 迁移证据与 Schema JSON；
- 版本历史写入独立表，只外键关联命例，避免删除当前条目时级联丢失；
- 完整备份格式仍为 v1；记录/分析/事件文件增加默认空历史数组，旧备份仍可恢复；
- manifest 新增两类历史计数，恢复校验引用、附件归属、重复 ID 和数量；
- 详情页可读取当前记录分类及全部记录/事件历史；编辑页提供统一分析分类；
- 研究副本继续不复制记录、事件、来源附件、字段证据及其历史；
- Debug 版本升级到 `0.3.0-alpha04`。

## 所有者与边界

- 唯一计算入口：`BaziEngine.calculate()`。
- 唯一增量写入口：`CaseRepository`。
- 唯一完整恢复入口：`CaseBackupService`。
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

- 本地自动化：60 条唯一单元契约；Debug/Release 变体合计 107 次执行，0 失败；
- 覆盖领域、引擎、Room v5 往返、v1→v5 迁移、旧 Schema v2 备份恢复、历史基线补建、
  删除保留历史、分析分类、生命周期用例、重复确认、ViewModel 和导航；
- `assembleDebug`：成功；
- `lintDebug`：成功，0 错误；App 8 条、数据模块 5 条依赖版本提示；
- API 35 模拟器 `ExpenseCapture_API35`：
  - Compose 自动化 1/1 通过，新增/修改分类分析并读取旧版本；
  - 同一流程还完成重复提示与确认→编辑→新增事件→复制→移入回收站→检索→恢复；
- OPPO 设备虽然连接，但未安装、未操作；以上证据不能替代真机验收；
- 真实问真迁移：未执行。

## APK

`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha04-debug.apk`

大小：9,780,678 bytes

SHA-256：`cb07ac7ef84a3b84b2a0cfd3280fbe6156188a8c09fe4bb5cabeea3c3a42768e`

该 APK 是 Debug 验收构建，不是正式签名 Release。

## 当前限制与风险

- 关键事件已有版本历史，但仍缺结构化标题和分类；
- 软删除没有永久清理入口，这是数据安全选择，不是静默遗漏；
- 副本有意不复制记录和附件，完成反馈已明确范围；
- 编辑器仍只支持公历民用时；农历、地区、时区确认和真太阳时未接通；
- 单命例 JSON、加密备份、非空库冲突预览和两阶段提交尚未实现；
- 没有进程重启、OPPO Find N5、真实 ZIP 或真实问真样本证据。

## 下一安全增量

继续 Stage 3A：

1. 为关键事件补齐结构化标题和分类，并随版本历史、Room、备份完整往返；
2. 补齐事件时间线的分类展示和回归；
3. 完成后进入 Stage 3B 的单命例 JSON、加密 ZIP 和非空库冲突预览。

`docs/REQUIREMENT_GAP_AUDIT.md` 是 v1.0 的逐项事实清单；任何单阶段完成都不能
表述为全项目落地。
