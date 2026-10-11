# 南枫八字 Android 项目 Context

## 项目身份

- 产品：南枫八字 Android，面向命例录入、排盘、专业细盘、断事记录、可控 AI 点评、备份与本机加密云备份。
- 当前代码基线：`main` 的 `2d5871f57fe20f2e6753a093f17d2c9c6a7e389f`，其核心源码与已发布 `v1.0.16` 标签的 `c2c4da75a1cb92a518f4819abfd5c897e97dcf12` 一致；动态验证与同步快照见 [PROJECT_STATUS.md](PROJECT_STATUS.md)。
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
- 专业岁运事实只由ProfessionalFortuneResolver生成；CaseCatalogStore为可丢弃读取投影，恢复／维护／写盘分离；专业视口状态由案例级持有，导出仍复用真实渲染器。
- 取消是正常控制流：页面离开、应用更新或 WorkManager 停止不得被转换成网络失败或内部异常文案。

## 构建

- 使用Android Studio JBR运行JVM／静态门禁：`./gradlew test lintDebug lintRelease`。APK生成只在产物确需变更时执行，冻结后checkpoint不重生成。
- 当前交付为 [v1.0.16 Release](https://github.com/nanzhufeng/NanfengBazi-Android/releases/tag/v1.0.16)，非Debug、沿用已验证原签名；APK SHA256 `3a5f9f37a56e85ae13416300991bb4936c98595bb58966e50547f010a6f4fcae`。

## 数据安全

- OPPO 只允许同签名主 APK 覆盖安装；不得卸载、清数据、安装测试 APK 或运行 instrumentation。
- 不记录或提交真实账号、恢复码、令牌、密钥、命例文本、附件与云端密文。
- 真机涉及真实上传、覆盖或恢复须在单独获得明确授权后执行；安装或构建通过不构成云服务验收。

## 风险

- 真实 Google/Supabase 上传、回读与跨设备恢复尚未在本 checkpoint 执行，不能标记为服务端已验收。
- 当前真机已完成同签名覆盖与数据目录保持核验，但为避免写入用户云端，本轮没有启动应用或主动触发同步。
- 临时生成、采样和验收目录不属于源码checkpoint；真实服务与全面真机交互仍为验证缺口。
