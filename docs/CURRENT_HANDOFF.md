# 当前交接：Stage 3B 单命例交换第一增量

更新日期：2026-07-30

## 当前结论

- Stage 0、Stage 1、Stage 2 与 Stage 3A 已有当前实现和证据，Stage 3B 正在进行。
- 本增量完成单命例 JSON v1 文件合同、确定性导出和零写入冲突预览服务。
- v1.0 总方案仍未完成；系统文件入口、冲突决策/提交、密码加密和两阶段恢复仍待落地。
- 未使用真实问真资料、真实姓名或用户截图；未安装或操作 OPPO 真机，未 push、未发布。

## 本增量实现

- 新增 `SingleCaseDocument` 格式 v1，封装完整 `BaziCase`、格式/App/数据库版本、
  ISO 导出时间、附件模式和规范载荷 SHA-256；
- 单 JSON 附件模式固定为 `REFERENCES_ONLY`，保留图片引用事实但明确不含图片二进制；
- 导出使用稳定文件名清洗规则，同时返回字节数和整文件 SHA-256；
- 未加密导出必须显式传入敏感数据风险确认；密码模式未实现时零输出并明确拒绝；
- 预览使用 16 MiB 有界读取，严格拒绝非法 JSON、未知格式/Schema、非法领域引用、
  元数据错误和载荷哈希不一致；
- 预览通过 `CaseRepository` 合并稳定 ID、出生输入、四柱和回收站冲突理由，全程零写入；
- 单命例交换不直接访问 DAO，不改变 Room Schema v5 或完整备份格式 v1；
- Debug 版本升级到 `0.3.0-alpha06`。

## 所有者与边界

- 唯一计算入口：`BaziEngine.calculate()`。
- 唯一增量写入口：`CaseRepository`。
- 唯一完整恢复入口：`CaseBackupService`。
- 唯一单命例交换入口：`SingleCaseExchangeService`；预览阶段严禁调用保存。
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

- 本地自动化：66 条唯一单元契约；Debug/Release 变体合计 119 次执行，0 失败；
- 覆盖领域、引擎、Room v5 往返、v1→v5 迁移、旧 Schema v2 备份恢复、历史基线补建、
  删除保留历史、单命例往返/哈希/版本/大小/明文确认/冲突零写入、生命周期用例、
  重复确认、ViewModel 和导航；
- `assembleDebug`：成功；
- `lintDebug`：成功，0 错误；App 8 条、数据模块 5 条依赖版本提示；
- API 35 模拟器 `ExpenseCapture_API35`：
  - Compose 自动化 1/1 通过，新增/修改分类分析并读取旧版本；
  - 同一流程还完成重复提示与确认→编辑→新增带标题/事业类别事件→详情及历史读回
    →复制→移入回收站→检索→恢复；
- 本增量没有新增 App 页面入口，因此未把上一增量 Compose 证据冒充文件选择或导入验收；
- OPPO 设备虽然连接，但未安装、未操作；以上证据不能替代真机验收；
- 真实问真迁移：未执行。

## APK

`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha06-debug.apk`

大小：9,670,718 bytes

SHA-256：`156898e3cb0a82ae89b6cba1e993a4e909a47a34fa1711a70f6ea447a2e6e1c7`

该 APK 是 Debug 验收构建，不是正式签名 Release。

## 当前限制与风险

- 软删除没有永久清理入口，这是数据安全选择，不是静默遗漏；
- 副本有意不复制记录和附件，完成反馈已明确范围；
- 编辑器仍只支持公历民用时；农历、地区、时区确认和真太阳时未接通；
- 单命例 JSON 尚无系统文件选择、用户预览或提交入口；
- `REFERENCES_ONLY` 不携带附件二进制，提交策略必须避免生成缺失文件的伪完整引用；
- 加密备份、四种冲突决策、非空库两阶段提交和进程强杀恢复仍未实现；
- 没有进程重启、OPPO Find N5、真实 ZIP 或真实问真样本证据。

## 下一安全增量

继续 Stage 3B：

1. 接通 Android 系统创建文档/打开文档入口，以及明文风险确认和逐命例预览 UI；
2. 定义跳过、保留两份、按模块合并、逐字段采用的差异模型，先实现无附件安全路径；
3. 再补密码加密、附件事务、两阶段提交、失败回滚和进程重启恢复日志。

`docs/REQUIREMENT_GAP_AUDIT.md` 是 v1.0 的逐项事实清单；任何单阶段完成都不能
表述为全项目落地。
