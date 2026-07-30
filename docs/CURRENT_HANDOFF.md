# 当前交接：Stage 2 手机单列最小业务闭环

更新日期：2026-07-30

## 当前任务

- 目标：完成“手动输入出生信息 → 排盘 → 保存 → 列表/搜索 → 详情”的真实闭环。
- 已修改范围：`app` 组合根、手动新建用例、ViewModel、手机单列页面、导航、
  仓储摘要读取、自动化测试和阶段文档。
- 未修改且受保护：Room Schema v2、备份格式 v1、空库恢复语义、真实问真截图、
  真实案例、非空库合并、密码加密、AI 和云同步。

## 已实现

- `DefaultAppContainer` 在应用组合根组装 `RoomCaseRepository` 与
  `TymeBaziEngine`；
- `CreateCaseUseCase` 是手动新建的完整用例：表单校验 →
  `BaziEngine.calculate()` → 已采用计算快照 → `CaseRepository.save()`；
- 当前表单只开放引擎已支持的公历、北京时间民用时，不为农历或真太阳时做静默降级；
- 必填项、无效日期、计算失败、稳定 ID 已存在、修订冲突和数据库异常均有明确中文反馈；
- 数据库异常后表单输入保留，不把保存失败误报为成功；
- 列表显示姓名/别名、性别、公历出生时间和已采用四柱；
- 搜索按姓名或别名读取 `CaseRepository.search()`；
- 详情通过稳定 ID 读取 `CaseRepository.findById()`，明确分为“原始录入信息”和
  “计算结果”，并显示计算配置、引擎版本、规则版本、四柱、起运与大运摘要；
- `CaseSummary` 增加出生输入和已采用四柱，但没有修改 Room Schema 或备份协议。

## 验证等级

完整自动化命令：

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
  ./gradlew test assembleDebug lintDebug --warning-mode all
```

- 已实现：Stage 2 代码、测试和项目事实文档已更新。
- 定向契约：
  - App 表单、用例、ViewModel 和导航 10 条，在 Debug、Release 两个变体运行；
  - 领域模型 5 条；
  - Tyme4j 适配器 6 条；
  - 数据模块 10 条，在 Debug、Release 两个变体运行；
  - 共执行 51 条 JVM/Android 本地自动化测试，0 失败。
- 构建：`assembleDebug` 成功，生成版本化 Debug APK。
- Lint：`app` 与 `core:data` 均 0 错误；仅有 8 条和 5 条依赖升级提示。
- 模拟器：
  - `ExpenseCapture_API35`，Android API 35；
  - Compose UI 测试 1/1 通过；
  - 使用合成资料和真实 Room + Tyme4j 完成新建→排盘→保存→搜索→详情。
- 真机：未执行；没有安装到 OPPO，也没有真实数据保留证据。
- 真实问真资料：未使用、未导入、未提交。

APK：
`app/build/outputs/apk/debug/NanfengBazi-Android-v0.2.0-alpha01-debug.apk`

大小：9,553,440 bytes

SHA-256：`0ab085eb046265bfe113e1148e36cec1e06821ab96f798cf8d74b22e4ce1696b`

该 APK 是 Debug 验收构建，不是正式签名 Release。

## 当前限制与风险

- 仅支持公历、民用时；农历、地区经纬度、历史时区和真太阳时尚未接通；
- 详情尚未覆盖完整基础排盘字段，列表尚无分组、标签、星标、置顶和排序；
- 尚无编辑、删除、复制、记录编辑和事件时间线 UI；
- 单命例 JSON、备份恢复和设置页尚未接入用户界面；
- 非空库恢复冲突合并、密码加密和跨数据库/附件崩溃日志尚未实现；
- 没有进程重启后的设备级持久读取验证；
- 没有 OPPO Find N5 手机/展开态视觉与数据保留验收；
- 问真 P0 截图迁移、OCR、导入会话和真实样本验收均未开始。

## 下一步

先对 `PRODUCT_REQUIREMENTS.md`、本交接和决策日志做 requirement-by-requirement
差距审计，建立可追踪的后续阶段清单。然后优先推进不依赖真实隐私资料、账号、密钥、
发布或不可逆外部变更的最高价值阶段；Stage 2 完成不代表项目已经落地。
