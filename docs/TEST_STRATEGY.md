# 现行测试与验收合同

> 本文拥有门禁选择和证据解释。根AGENTS拥有授权/主设备安全；[合同索引](CONTRACT_INDEX.md)用于定位领域/数据/云规则。[历史门禁](history/contracts/TEST_STRATEGY-before-20261012.md)保留阶段测试名、像素、任务及旧PASS，不是当前运行许可。

## 证据按层报告

| 层 | 能证明什么 | 不能替代什么 |
| --- | --- | --- |
| 静态/合同/配置 | 所有者、链接、敏感模式、配置与结构一致 | 执行正确或真实服务可用 |
| JVM/本地数据行为 | 合成/公开样本、Robolectric Room/迁移/恢复、状态与边界 | 所有历史用户库、目标设备渲染/网络 |
| 构建/Lint/验签 | 代码可编译、特定变体与实际包身份 | UI/业务验收、服务端配置 |
| 隔离模拟器 | 合成资料下真实入口、返回/交互/导出/窗口变化 | 主设备/OEM全部行为 |
| 主设备覆盖/黑盒 | 授权动作范围的精确包与数据保持/交互 | 自动取得新授权或证明真实服务完成 |
| 真实服务/资料 | 对应身份、输入、实际上传/回读/恢复结果 | 未覆盖账号、设备、全量算法正确 |

## 安全门禁

- 永久禁止执行任何connected*AndroidTest，包括通过设备号/脚本绕过；旧聚合命令不得照搬。
- 主设备禁止Debug/测试/辅助APK、instrumentation、自动启动、卸载/清数据/直改数据库；授权同签名Release覆盖按根AGENTS核验，正常更新沿用现装匹配原身份。
- 仅在隔离且可识别的自有模拟器，按明确任务执行合成资料验收；仓库历史脚本/测试源码存在不授予运行权限，必须先核对脚本目标与清理边界。真实服务/用户资料另行授权。

## 选择最小充分门禁

- 文档/合同：`python3 scripts/check_contract_consistency.py`与`python3 -m unittest discover -s scripts -p test_check_contract_consistency.py`、相对链接/JSON/Skill校验、`git diff --check`，核对业务源码/配置未变。复核冻结测试可以，必须保留原运行时间，不重建APK。
- 日常行为：先运行匹配模块和失败模式的JVM测试；需要构建时选受影响变体，不为低影响可回滚调整添加仅镜像实现的测试。
- 交付或广泛影响：使用Android Studio JBR和wrapper执行 `./gradlew test lintDebug lintRelease`；APK确需变更才assemble对应变体并验签。冻结产物不为文档/checkpoint重建。
- 要证明JVM实际重跑时加 `-I scripts/fresh_jvm_tests.init.gradle`；从XML读取数量、失败/错误/跳过和suite timestamp，不把旧报告mtime或Gradle退出0当新执行证明。
- 失败先定位首个可判定红灯，保留日志并重跑同一信号；不无目的扩大平台/测试范围，不拿清用户数据作修复。

## 按所有者验证行为

| 风险 | 主要回归资产/场景 |
| --- | --- |
| 算法与时间 | TymeBaziEngine/ProfessionalFortuneResolver/Lookup测试、GOLDEN_CASES；公农历/闰月、立春/交节前后、两子时口径、DST重叠/空洞、真太阳跨日/时辰、历法支持边缘 |
| provider隔离 | 正向不修改全局；反查在受锁作用域设置并finally恢复provider；交替规则调用不污染结果 |
| 专业五层 | 节令月十二项、跨交运完整候选、流日/时交集、默认23点次日、秒锚点回读、每个点选等于重新定位、童限立春前年度 |
| 数据/Schema | DatabaseMigrationTest、RoomCaseRepositoryTest；expectedRevision/三态/顺序/库类型、完整迁移链、软删除/恢复/永久删除隔离 |
| 备份/交换 | CaseBackupService/Exchange/Bundle行为测试；零写入预演、篡改/路径/容量/数量/引用拒绝、显式计划过期、密码再认证、附件失败回滚与journal中断 |
| 目录/详情 | CaseCatalogStoreTest、StageTwoViewModelTest；挂起维护/写盘仍发布，旧代次不覆盖，新案例子状态一致，标签/返回/宽窄保持观察与轴状态 |
| 导入/OCR | image-parser及ScreenshotImport相关测试；边界框/分块/分组、来源保真、专业九列/反查一致、上传确认/失败、人工核对、取消/恢复会话；本地fixture不证明生产AI识别成功 |
| AI/云 | Prompt/Generator/Cloud策略与加密/快照测试；范围/隐私/采用、密文篡改、身份/账号切换、启用后后台/本机源/空白恢复、取消/重试/本地变化代次 |
| UI/导出 | InteractionShapeContractTest等静态合同 + 目标渲染/连续点选；形状、命中/尾部、页间返回、合盘个案关系、真实长图/文件交付及finally恢复 |

## 性能、视觉与真实验收

记录冷热入口、实际包/设备/API、窗口/字体配置、优化生效状态和宿主负载。帧慢帧、启动耗时、点击响应、uiautomator可访问性空闲不是同一指标；不能相互替代或承诺全设备零延迟。

Profile文件存在不证明安装/转码/平台编译生效；检查实际状态后才归因性能改善。源码UI合同不能证明60fps；截图不能证明业务完成，历史截图只在相同代码/布局/输入范围适用。

真实AI视觉、点评、Google/Supabase、上传/回读/恢复和真实问真逐字段对账分别验收；未执行写NOT RUN/UNKNOWN，不能从依赖、配置、模拟器或安装成功推断。PROVISIONAL口径仍保留外部验证缺口。

## 报告与维护

每项保存代码范围、命令、运行时间、环境、结果、失败/跳过原因、可追溯证据和限制。Android变体重复执行项不称独立业务场景。当前执行结果/版本/同步快照只更新PROJECT_STATUS与验证报告，本合同不滚动复制测试总数或旧阶段完成表。

## 立春年界回归

- `LichunBoundaryRegressionTest` 覆盖2000固定案例、1901—2100各年T−1天／分钟／秒、T、T+秒／分钟／天、五组年份全部十二节与十二气、同Instant跨时区、设备默认时区、真太阳时经度、历史上海offset、分钟／时辰歧义和海外反查。
- `lichun-reference-1901-2100.csv` 是开发期独立参考冻结表；1950—2050差异>5秒要求复核，其他范围只记录模型差异，不以放宽断言声称已达秒级精度。生成器、hash、实际星历flag及许可边界见[专项验收](lichun-year-boundary-20261026.md)。
- App经真实ViewModel验证选中分钟／秒、歧义禁采用、北京时间带入及瞬时预览零持久化；数据层验证旧采用快照与新年界证据共存的Room／备份恢复。

- `CaseYearZodiacNoticeTest` 覆盖正常旧盘静默、年柱/属相分别或同时异常、缺证据/属相、农历/海外时区及合法交节候选；核对不得改写原快照。`CaseIdentityZodiacTest` 与 `CaseCatalogZodiacTest` 覆盖详情和目录共享的星座补齐。
