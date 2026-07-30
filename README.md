# 南枫八字

南枫八字是面向个人案例管理与结构化研判的本地优先 Android 应用。

当前仓库已完成 **Stage 0 算法骨架** 和 **Stage 1 数据底座**：

- 定义出生输入、计算口径和版本化结果；
- 通过唯一入口 `BaziEngine.calculate()` 调用计算引擎；
- 使用 Tyme4j 1.5.1 作为首个适配器；
- 以公开样本建立可回归的黄金用例；
- 建立 Room Schema v2、统一命例仓储和修订冲突保护；
- 建立版本化 ZIP 备份、附件哈希校验和空库恢复回滚；
- 提供一个明确标注“尚未进入业务界面开发”的占位应用。

问真截图迁移、正式界面、非空库冲突合并和密码加密尚未实现。详细范围见
[`docs/PRODUCT_REQUIREMENTS.md`](docs/PRODUCT_REQUIREMENTS.md) 与
[`docs/CURRENT_HANDOFF.md`](docs/CURRENT_HANDOFF.md)。

## 本机构建

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug lintDebug
```

Debug APK 由 `app` 模块生成；未完成设备实测前，不应把“构建成功”表述为“产品可用”。
