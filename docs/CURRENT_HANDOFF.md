# 当前交接：Stage 3A 命例管理第二增量

更新日期：2026-07-30

## 当前结论

- Stage 0、Stage 1、Stage 2 与 Stage 3A 前两个受控增量已有当前实现和证据。
- 本增量补齐分组、标签、收藏、置顶、最近查看、四柱搜索、筛选与四种排序。
- v1.0 总方案仍未完成；Stage 3A 尚缺整例删除、命例复制和记录级历史。
- 未使用真实问真资料、真实姓名或用户截图；未安装真机、未 push、未发布。

## 本增量实现

- `BaziCase` 正式保存 `isFavorite`、`isPinned` 和 `lastViewedAt`；
- Room 升级到 Schema v3，并提交 v1→v2→v3 迁移证据和 Schema JSON；
- 新字段带序列化默认值，旧备份格式 v1 仍可解码，空库恢复语义不变；
- `CaseRepository.search(CaseSearchRequest)` 成为列表搜索、筛选和排序的唯一入口；
- 搜索覆盖姓名、别名和四柱；分组、标签可组合筛选；
- 支持最近查看、最近更新、最近创建和出生时间排序，置顶始终优先；
- `markViewed()` 只更新查看时间，不提升聚合 revision，避免浏览详情制造编辑冲突；
- 分类编辑复用同名分组/标签，校验名称长度和数量，通过 `CaseRepository.save()` 写入；
- 列表展示收藏、置顶、分组和标签；详情增加独立“命例管理”区；
- 分类编辑页保持手机单列，保存冲突和数据库异常保留输入并给出中文反馈；
- Debug 版本升级到 `0.3.0-alpha02`。

## 所有者与边界

- 唯一计算入口：`BaziEngine.calculate()`。
- 唯一增量写入口：`CaseRepository`。
- 唯一完整恢复入口：`CaseBackupService`。
- 页面不直接访问 DAO，不复制搜索排序或持久化规则。
- 最近查看是非业务内容元数据，不修改 `updatedAt` 或 revision。
- 当前出生时间排序对公历命例是时间顺序；农历跨历法统一排序需随农历能力补齐。

## 当前验证证据

自动化命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew testDebugUnitTest assembleDebug lintDebug --console=plain
```

- 本地自动化：78 条，0 失败；
- 覆盖领域、引擎、Room 往返、v1→v3 迁移、备份恢复、用例、ViewModel 和导航；
- `assembleDebug`：成功；
- `lintDebug`：成功，0 错误；
- API 35 模拟器 `ExpenseCapture_API35`：
  - Compose 自动化 1/1 通过；
  - 使用合成资料和真实 Room + Tyme4j 完成
    新建→搜索→详情→编辑重算→分类/收藏/置顶→记录→事件→详情读回；
- 真机：未执行；以上证据不能替代 OPPO 验收；
- 真实问真迁移：未执行。

APK：
`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha02-debug.apk`

大小：9,709,473 bytes

SHA-256：`2b82ccfa3b8ec5eacb66adf0293a80d6486cbdf21593db11c8f3c201c7c083de`

该 APK 是 Debug 验收构建，不是正式签名 Release。

## 当前限制与风险

- 整例删除尚无恢复语义；必须先确定附件清理、备份和误删恢复策略；
- 命例复制和重复命例候选提示尚未实现；
- 文本记录和事件没有记录级版本历史，分析分类仍是通用模型；
- 编辑器仍只支持公历民用时；农历、地区、时区确认和真太阳时未接通；
- 出生时间排序暂按输入历法结构键；农历启用前需改为统一时间轴；
- 单命例 JSON、加密备份、非空库冲突预览和两阶段提交尚未实现；
- 没有进程重启、OPPO Find N5、真实 ZIP 或真实问真样本证据。

## 下一安全增量

继续 Stage 3A：

1. 先在决策日志明确整例删除采用“软删除 + 延迟物理清理”还是受控物理删除；
2. 实现确认删除、列表隐藏、恢复/清理边界与附件引用测试；
3. 实现命例复制和出生输入/四柱重复候选提示；
4. 再补记录级历史与分析分类，完成 Stage 3A 后进入 Stage 3B。

`docs/REQUIREMENT_GAP_AUDIT.md` 是 v1.0 的逐项事实清单；任何单阶段完成都不能
表述为全项目落地。
