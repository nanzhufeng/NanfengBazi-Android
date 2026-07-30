# 南枫八字项目执行约束

## 开始前必读

1. `docs/PRODUCT_REQUIREMENTS.md`
2. `docs/architecture-governance.md`
3. `docs/domain-rules.md`
4. `docs/CURRENT_HANDOFF.md`

实现、重构或诊断本项目时，使用 `developing-apps-with-governance` 与
`develop-apps-with-nanfeng-product-standards` 两项 Skill。

## 当前边界

- 当前阶段已完成 Stage 1：案例数据底座与空库恢复协议。
- 唯一计算入口是 `BaziEngine.calculate()`；界面层不得直接调用历法库。
- Tyme4j 的全局 `ChildLimit.provider` 只能由 `engine-tyme` 适配器访问。
- 命例写入只能通过 `CaseRepository`；完整恢复只能通过 `CaseBackupService`。
- 当前恢复只允许空数据库；密码加密未实现时必须明确拒绝。
- 暂不实现问真截图识别、真实案例导入或正式业务界面。
- 仓库不得提交用户真实八字、姓名、截图、密钥、构建产物。

## 验证命令

本机应使用 Android Studio 随附 JBR：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug lintDebug
```

报告时分开说明：静态检查、单元测试、构建、设备实测、真实数据验收。
