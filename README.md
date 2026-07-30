# 南枫八字

南枫八字是面向个人案例管理与结构化研判的本地优先 Android 应用。

当前仓库已完成 **Stage 0 算法骨架**、**Stage 1 数据底座**和
**Stage 2 手机单列最小业务闭环**及 **Stage 3A 前两个命例管理增量**：

- 定义出生输入、计算口径和版本化结果；
- 通过唯一入口 `BaziEngine.calculate()` 调用计算引擎；
- 使用 Tyme4j 1.5.1 作为首个适配器；
- 以公开样本建立可回归的黄金用例；
- 建立 Room Schema v3、兼容迁移、统一命例仓储和修订冲突保护；
- 建立版本化 ZIP 备份、附件哈希校验和空库恢复回滚；
- 手动录入公历出生资料，经 `BaziEngine.calculate()` 排盘后通过
  `CaseRepository` 保存；
- 在手机单列界面中完成命例列表、姓名/别名搜索和详情查看；
- 对必填校验、计算失败、保存冲突和数据库异常提供明确中文反馈。
- 支持按修订号编辑命例并重新排盘，保留旧计算快照；
- 支持普通笔记、命主反馈、师傅点评、分析记录和关键事件的增改删。
- 支持分组、标签、收藏、置顶、最近查看、四柱搜索、筛选和多种排序。

当前 UI 已覆盖最小真实闭环和主要命例管理元数据。农历、地区/真太阳时、整例删除、
命例复制、问真截图迁移、
非空库冲突合并和密码加密尚未实现。详细范围见
[`docs/PRODUCT_REQUIREMENTS.md`](docs/PRODUCT_REQUIREMENTS.md) 与
[`docs/CURRENT_HANDOFF.md`](docs/CURRENT_HANDOFF.md)。v1.0 逐项差距与后续阶段见
[`docs/REQUIREMENT_GAP_AUDIT.md`](docs/REQUIREMENT_GAP_AUDIT.md)。

## 本机构建

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug lintDebug
```

Debug APK 由 `app` 模块生成，名称包含平台、版本和 Debug 标记。模拟器验证与
OPPO 真机、真实问真资料验收必须分开报告。
