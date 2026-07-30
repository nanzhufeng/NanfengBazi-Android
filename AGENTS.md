# 南枫八字项目执行约束

## 开始前必读

1. `docs/PRODUCT_REQUIREMENTS.md`
2. `docs/architecture-governance.md`
3. `docs/domain-rules.md`
4. `docs/CURRENT_HANDOFF.md`

实现、重构或诊断本项目时，使用 `developing-apps-with-governance` 与
`develop-apps-with-nanfeng-product-standards` 两项 Skill。

## 当前边界

- 当前阶段已完成 Stage 3A 五个增量：Stage 2 闭环加命例编辑重算、文本记录、
  关键事件、分类、收藏/置顶、最近查看、搜索筛选与排序，以及可恢复删除、命例复制
  和重复候选提示，并已落地记录/事件版本历史、统一分析分类及事件标题/类别。
- 下一阶段是 Stage 3B 数据安全；不得跳过单命例交换、加密备份、非空冲突预览和
  两阶段恢复门禁。
- Stage 3B 已接通单命例 JSON v1 封套、系统文件、零写入预览，以及无附件命例的跳过、
  保留两份、按模块追加和逐字段采用；附件模式仅引用，带附件命例仍禁止提交。
- 唯一计算入口是 `BaziEngine.calculate()`；界面层不得直接调用历法库。
- Tyme4j 的全局 `ChildLimit.provider` 只能由 `engine-tyme` 适配器访问。
- 命例写入只能通过 `CaseRepository`；完整恢复只能通过 `CaseBackupService`。
- 完整 ZIP 已支持明文/密码文件的逐例计划与非空库恢复；必须经过只读预览、显式逐例决策、
  零写入方案检查和最终确认。
- 单命例 JSON 已支持版本化密码容器；页面不得保存密码或绕过交换服务自行解密。
- 完整 ZIP 已接通明文/密码系统文件导出和零写入预览；页面不得自行解析容器或开放
  非空库恢复。
- 完整备份预览已逐命例显示稳定 ID、出生输入和四柱冲突候选及本地修订号；该预览不得
  被当作恢复授权，提交前仍须重新核对并取得逐例决策。
- 完整备份恢复方案要求每个来源命例显式决策；无冲突只允许按原 ID 导入或跳过，冲突
  命例不得直接导入，空范围合并、回收站目标和过期预览必须拒绝。
- 整例删除只能使用软删除进入回收站；当前不得实现自动物理清理。
- 当前仍不实现问真截图识别、真实案例导入或正式视觉复刻；Stage 3B 先完成单命例可选
  附件包，之后再进入算法与截图迁移阶段。
- 仓库不得提交用户真实八字、姓名、截图、密钥、构建产物。

## 验证命令

本机应使用 Android Studio 随附 JBR：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug lintDebug
```

报告时分开说明：静态检查、单元测试、构建、设备实测、真实数据验收。
