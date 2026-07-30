# 当前交接：Stage 3A 结构化事件第五增量

更新日期：2026-07-30

## 当前结论

- Stage 0、Stage 1、Stage 2 与 Stage 3A 五个受控增量已有当前实现和证据。
- 本增量完成关键事件结构化标题和类别，Stage 3A 命例生命周期与记录闭环已收口。
- v1.0 总方案仍未完成；下一阶段是 Stage 3B 单命例交换与完整数据安全。
- 未使用真实问真资料、真实姓名或用户截图；未安装或操作 OPPO 真机，未 push、未发布。

## 本增量实现

- `CaseEvent` 新增可选 `title` 和 `CaseEventCategory`；类别固定为综合、学业、事业、
  财运、感情、家庭、健康、其他八类；
- 事件标题允许缺失，但非空时不能只含空白且最长 80 字；
- `EventDraft`、`CaseEventUseCase`、ViewModel 与编辑页完整接通标题/类别；
- 新增和修改事件时，标题/类别随当前事件及不可覆盖版本快照共同保存；
- 旧数据库或旧备份缺字段时按“无标题、综合类别”读取；因此 Room 继续使用 Schema v5，
  完整备份继续使用格式 v1，无需制造无意义迁移；
- 详情页当前事件与事件历史均展示日期精度、类别、标题、原文和状态；
- 事件编辑器使用独立滚动内容与固定底部保存按钮，长表单及软键盘场景仍可稳定提交；
- 自动化覆盖标题/类别保存、编辑、历史快照、80 字上限和旧默认值；
- Debug 版本升级到 `0.3.0-alpha05`。

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
  删除保留历史、分析/事件分类、事件标题、生命周期用例、重复确认、ViewModel 和导航；
- `assembleDebug`：成功；
- `lintDebug`：成功，0 错误；App 8 条、数据模块 5 条依赖版本提示；
- API 35 模拟器 `ExpenseCapture_API35`：
  - Compose 自动化 1/1 通过，新增/修改分类分析并读取旧版本；
  - 同一流程还完成重复提示与确认→编辑→新增带标题/事业类别事件→详情及历史读回
    →复制→移入回收站→检索→恢复；
- OPPO 设备虽然连接，但未安装、未操作；以上证据不能替代真机验收；
- 真实问真迁移：未执行。

## APK

`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha05-debug.apk`

大小：9,646,119 bytes

SHA-256：`78a6f08ebf5af5c9503795cd9a2202b8ab504f49ea45147d72f1e57eb458d77a`

该 APK 是 Debug 验收构建，不是正式签名 Release。

## 当前限制与风险

- 软删除没有永久清理入口，这是数据安全选择，不是静默遗漏；
- 副本有意不复制记录和附件，完成反馈已明确范围；
- 编辑器仍只支持公历民用时；农历、地区、时区确认和真太阳时未接通；
- 单命例 JSON、加密备份、非空库冲突预览和两阶段提交尚未实现；
- 没有进程重启、OPPO Find N5、真实 ZIP 或真实问真样本证据。

## 下一安全增量

进入 Stage 3B：

1. 先定义单命例 JSON 封套、稳定 ID/修订/附件引用和兼容版本；
2. 实现只读导入预览与非空库冲突分类，不在预览阶段写数据库或附件；
3. 再补密码加密 ZIP、两阶段提交、失败回滚及设备文件选择链路。

`docs/REQUIREMENT_GAP_AUDIT.md` 是 v1.0 的逐项事实清单；任何单阶段完成都不能
表述为全项目落地。
