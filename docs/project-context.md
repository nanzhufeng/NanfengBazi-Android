# 南枫八字 Android 项目 Context

## 项目身份

- 产品：南枫八字 Android，面向命例录入、排盘、专业细盘、断事记录、可控 AI 点评、备份与本机加密云备份。
- 当前基线：`main` 的 checkpoint `b16a14c`；源代码、测试、迁移、六套皮肤资源与项目规则已在该提交冻结。
- 数据原则：Room 是本机业务真值；云端只保存独立 App 标识下的加密结构化快照，不携带来源图片、附件、字段证据或模型密钥。

## 技术栈

- Kotlin、Jetpack Compose、Room、WorkManager、kotlinx.serialization 与 Android Keystore。
- 命理计算由 `core:domain`、`core:engine-tyme`、`core:solar-time` 分层；数据、备份与恢复由 `core:data` 统一拥有。
- Google 身份与南枫云走 `BaziCloudSyncCoordinator`；后台调度、加密与页面状态不得自行复制同步逻辑。

## 目录结构

- `app/`：Compose 页面、Android 集成、WorkManager、系统文件与账号界面。
- `core/domain/`：领域模型、用例合同与可测试的业务规则。
- `core/data/`：Room、仓储、迁移、备份/恢复与云快照序列化。
- `core/engine-tyme/`、`core/solar-time/`：排盘与时间计算实现。
- `docs/`：项目规则、决策、测试策略、同步合同与当前交接。

## 架构

- 详情状态以完整案例聚合为单位。`detail`、笔记草稿、已保存草稿与事件时间线必须共享 `caseId + revision`，由 `StageTwoViewModel` 原子装载；缓存只能加速首帧，不能证明子记录为空。
- 云快照由 `BaziCloudSnapshotBridge` 调用 `CaseBackupService.exportCloudSnapshot` 生成。大库 JSON 必须逐条流式编码，先计算清单哈希再写 ZIP，执行于 IO 调度器。
- 取消是正常控制流：页面离开、应用更新或 WorkManager 停止不得被转换成网络失败或内部异常文案。

## 构建

- JVM/构建/静态检查门禁：`JAVA_HOME=/Applications/Android\\ Studio.app/Contents/jbr/Contents/Home ./gradlew test assembleDebug lintDebug`。
- 本 checkpoint 的 Debug APK：`app/build/outputs/apk/debug/NanfengBazi-Android-v0.3.0-alpha83-debug.apk`，SHA-256 为 `f1282c80af15da73d0287ae1c4e567d8ae404537698706b39d1f6c15a697852a`。

## 数据安全

- OPPO 只允许同签名主 APK 覆盖安装；不得卸载、清数据、安装测试 APK 或运行 instrumentation。
- 不记录或提交真实账号、恢复码、令牌、密钥、命例文本、附件与云端密文。
- 真机涉及真实上传、覆盖或恢复须在单独获得明确授权后执行；安装或构建通过不构成云服务验收。

## 风险

- 真实 Google/Supabase 上传、回读与跨设备恢复尚未在本 checkpoint 执行，不能标记为服务端已验收。
- 当前真机已完成同签名覆盖与数据目录保持核验，但为避免写入用户云端，本轮没有启动应用或主动触发同步。
- `tmp/imagegen/` 是生成过程记录，不属于代码基线，也未纳入 checkpoint。
