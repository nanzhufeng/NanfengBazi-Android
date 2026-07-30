# 当前交接：Stage 0 算法验证骨架

更新日期：2026-07-30

## 已完成

- 建立 Android/Kotlin/Compose 最小工程；
- 建立 `core:domain` 与 `core:engine-tyme` 单向依赖；
- 定义结构化出生输入、计算配置、版本证据与结果模型；
- 固定唯一计算入口 `BaziEngine.calculate()`；
- 接入 Tyme4j 1.5.1，并隔离其进程级起运 provider；
- 建立公开四柱样本、立春边界、配置隔离和失败显式化测试；
- 将 v0.2 产品需求纳入仓库；
- 设置无网络权限、禁止系统备份和明文流量的 Stage 0 清单。

## 自动化证据

执行命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug lintDebug --warning-mode all
```

当前结果：

- 领域测试：2 条通过；
- Tyme4j 适配器测试：6 条通过；
- Android Lint：0 错误；3 条依赖更新提示，需在升级 SDK 阶段统一处理；
- Debug 构建：成功；
- APK：`app/build/outputs/apk/debug/app-debug.apk`，约 8.6 MB；
- APK SHA-256：`ccd7ab31d78024f134656ec360ae6e5cef3c95c36298399b7c361ebcbb5e1805`。

构建产物未纳入 Git，哈希应以每次最终构建后的现场结果为准。

## 尚未完成

- 未安装到真机，未做启动与兼容性验证；
- 未实现正式首页、案例列表、排盘详情或断事笔记；
- 未实现数据库、备份恢复、导出；
- 未实现问真截图 OCR、批量导入、字段复核；
- 未使用真实问真案例做逐字段比对；
- 未实现农历输入、历史时区和真太阳时。

因此当前只达到自动化与构建证据 L3，不能宣称“问真输出一致”或“产品可用”。

## 下一阶段建议

下一线程只进入 **Stage 1：数据底座与可恢复性**：

1. 固化案例、原始截图、结构化识别结果和人工修订的实体边界；
2. 先做本地数据库迁移、导出与恢复测试；
3. 用脱敏人工夹具验证可逆性；
4. 通过数据恢复门禁后，再进入问真截图迁移。

进入 Stage 1 前，应先确认数据库字段与产品需求 v0.2 的案例信息矩阵一致。
