## 2026-10-28 当前代码已建立本地checkpoint

- 代码提交：`226e5449f6ff5cc18279a0f5511c78a0c79294a7`（main）；54个代码/测试/脚本文件含16项新增，193项实际生产源码/配置已核对与已安装正式包和Git索引一致。[核验](verification/checkpoint-20261028.json)。
- 已有回归/构建/同签名覆盖证据保留原时间复用，本次只补文档/合同和Git检查；不再生成APK或重复安装。旧清单198项中的5项Finder元数据保持忽略，正式源码数为193。
- 原签名Release已于12:45覆盖，APK回读哈希026b47d5…一致；业务显示、真实服务与既有云恢复附件引用缺口仍按PROJECT_STATUS保留。
- `docs/next-codex-prompt.md`删除、`docs/可迁移开发经验.md`修改、`docs/流畅度优化复盘与通用规则.md`未跟踪内容不属于本次checkpoint，保留原状。未推送/发布。以下为之前阶段历史。

## 2026-10-28 提示修复正式版已覆盖OPPO

- 用户本轮明确授权正式打包、保数据覆盖。assembleRelease通过；原证书、非debuggable Release 1.0.16(10048)已以pm install -r --user 0覆盖PKH120，返回Success。
- 安装后回读APK SHA256：026b47d5c044e577b5aae4c4f93e27186147721c6c755c9a5261d11ab9d9066f；与冻结候选一致，签名复核匹配。appId、CE/DE目录inode、路径及首次安装时间不变；临时APK已精确清理并核验无残留。
- 冻结APK：/tmp/bazi-year-notice-overlay-20261028/NanfengBazi-Android-v1.0.16-release.apk；198项生产源码/配置输入摘要未变。完整回执：/tmp/bazi-year-notice-overlay-20261028/result.json；[专项验收](verification/year-zodiac-notice-20261028.json)追加本次安装证据，原JVM/Lint运行时间不刷新。
- 未启动/自动化操作主设备、未直读写业务数据库、未卸载/清数据；真实命例的提示显示仍待用户正常使用确认。以下未安装描述是本轮覆盖前历史状态。

## 2026-10-28 年柱/属相异常提示

- 用户要求移除通用“历史快照”提示，只在年柱或属相确有问题时显示。CaseYearZodiacNotice投影调用TymeBaziEngine.verifyYearAndZodiac，复用历法/历史offset/交节解析；不计算整盘岁运、不改写旧采用快照。
- 详情remember绑定采用快照，后台Dispatcher执行；切换快照不复用旧提示。正常旧盘、缺属相/证据、无法核实及合法立春候选不误报；错误时显示具体应有年柱/属相。
- 12项回归先红后绿；相关完整App/引擎423项（422通过、1既有跳过），Debug构建、Release Kotlin编译、Debug/Release Lint通过。源码摘要与命令见[专项验收](verification/year-zodiac-notice-20261028.json)，原始日志/修改前精确备份在/tmp/bazi-year-notice-20261028/。
- 本轮未模拟器/主设备渲染验证，未安装/提交/推送/发布；既有WIP保持。后续若要求覆盖须另按当次授权准备同签名Release，不能安装本轮Debug APK。

## 2026-10-26 年界修复包已覆盖OPPO

- 用户授权覆盖，沿用原签名非debuggable Release 1.0.16(10048)，pm install -r --user 0返回Success；包含本轮年界/万年历修复。
- 覆盖前后appId、CE/DE目录inode、路径和首次安装时间不变；安装后APK回读SHA256一致：7bee23ae6c6354dcc71473ef1206046fd05e10f3f036944e6c4100ed76dc70b8。180项生产源码/构建配置摘要未变。
- 冻结包与证据：/tmp/bazi-lichun-overlay-20261026/。未卸载、清数据、启动或自动化操作主设备；真实业务交互仍未验证。以下“未安装”为覆盖前历史。

## 2026-10-26 立春年界与万年历专项

- 本轮按用户附件方案执行，当前主引擎2000-02-04 08:00原已正确；红灯来自万年历整日摘要、海外时区与未知秒交节分钟。共享TymeSolarYearMonthResolver、版本化证据、万年历分秒/范围、反查、截图多快照提交及资料库跨规则保留已修复。
- 1063项全新JVM（0失败/错误、2原有跳过）、Debug/Release构建及Lint通过。命令、日志、修改清单、独立星历与范围见 [验收](verification/lichun-year-boundary-20261026.json) 和 [分析报告](lichun-year-boundary-20261026.md)。
- 独立API35模拟器启动超时，新增秒选择器渲染验收未通过；日志见/tmp/bazi-lichun-20261026/ui，已停止本任务模拟器并清理临时XML。不得把此前模拟器证据当本轮通过。
- 年柱旧采用盘不会自动替换；已有编辑出生资料/重排入口显式生成新结果。名人约略时间跨节只保留来源，不伪造确定采用盘；完整574条预检通过。未运行connected任务、未操作主设备、未安装/推送/发布。
- 所有此前WIP保留；代码底层HEAD仍66f95c3，当前WORKTREE未提交。本轮前态在/tmp/bazi-lichun-20261026/baseline.patch和untracked-baseline，回滚只撤本轮，不reset/clean工作区。
- 额外缺口：合成导入来源样本发现既有云快照剔除附件但未清理笔记/事件引用，恢复返回CROSS_CASE_ATTACHMENT_REFERENCE。本轮不混改云同步，日志/tmp/bazi-lichun-20261026/backup-regression.log；后续独立修复应覆盖历史版本引用。主设备759条真实命例未扫描。

以下10月25日及之前安装记录均为历史，本轮年柱包尚未覆盖。


## 2026-10-25 详情星座修复已覆盖OPPO

- 用户授权后，assembleRelease + lintRelease通过；原签名非debuggable Release 1.0.16(10048)已以pm install -r --user 0覆盖PKH120，返回Success。
- 覆盖前后appId、CE/DE目录inode、路径和首次安装时间一致；安装后APK回读SHA256与冻结候选一致：fca73621738d153199e19762ca555aafb40d49203c3ed0c3ec747070cf610184。源码167文件摘要与构建前一致。
- 证据及冻结包：/tmp/bazi-identity-overlay-20261025/；构建日志/tmp/bazi-identity-zodiac-release-20261025.log。未卸载/清数据、未启动或UI自动化操作主设备；业务真实路径仍待用户实际操作。
- 以下未Release打包/未覆盖描述为覆盖前历史状态；未修改桌面图标或发布GitHub。

## 2026-10-25 最新：详情身份栏星座遗漏修复

- CaseZodiacPresentation.kt（新增）统一目录与详情补齐规则；StageTwoScreens.kt身份栏remember复用出生公历/农历和规范日期，无完整排盘、无持久化改写；无效日期显示未识别。
- CaseIdentityZodiacTest.kt新增6项，红灯4失败已复现；修复后相关213项及Debug构建通过。API35合成旧样本缺basicChartDetails，基本信息/基本排盘/专业细盘显示正确星座。证据docs/verification/case-identity-zodiac-20261025.json。
- 此修复未Release打包/未覆盖手机；当前工作区与此前UI/文档WIP均未提交。以下10月22日安装记录为历史。


## 2026-10-22 模型设置/关于白底版已覆盖

- assembleRelease + lintRelease通过，沿用58项定向测试与两皮肤模拟器核对证据。
- OPPO PKH120同签名非debuggable Release 1.0.16(10048)以pm install -r --user 0覆盖Success；appId、CE/DE目录inode、路径与首次安装时间不变，APK回读哈希一致。未启动/自动化操作App、未卸载/清数据。
- 当前现装SHA256：56997e7e7a199d35bfcb57f91c466a04c7fe67d8974d8ba3ec5abf482186c22e；候选及安装证据：/tmp/bazi-white-fields-overlay-20261022/；构建日志/tmp/bazi-white-fields-release-20261022.log。
- 以下未覆盖说明是覆盖前历史状态；本轮没有实际云同步验收或GitHub发布。

## 2026-10-22 最新增量：模型设置/关于白底

- AiCommentaryDialogs.kt共享模型表单：API Key白底/普通边框透明、模型框纯白无阴影、取消/保存白色胶囊；StageTwoScreens.kt关于卡片阴影2→0且无外边框。
- 58项定向测试与Debug构建通过，日志/tmp/bazi-white-fields-about-20261022.log。此增量未生成Release/未覆盖手机，现装仍为下述57e553a0…包。
- 其他WIP保留；详情与视觉证据范围见ui-refresh-20261021.md末节。


## 2026-10-22 本轮候选已覆盖OPPO

- 用户授权覆盖后，57e553a0de11570d68fdf3ce6ccb0051a29f96dccf75915466f75310259a4f57候选已同签名Release保数据覆盖PKH120；pm install -r --user 0返回Success。
- 覆盖前后appId、CE/DE数据目录inode、数据目录路径与首次安装时间一致；安装后APK拉回SHA256与候选一致。未卸载/清数据，未启动或自动化操作App；真实账号同步和业务交互仍待验证。
- 证据：/tmp/bazi-ui-cloud-overlay-20261022/result.json及该目录安装前后元数据/日志。以下“未覆盖”描述为覆盖前历史状态。

## 2026-10-22 最新：功能图标与南枫云

- 当前WORKTREE已完成无装饰底块的共享功能图标（新BaziFeatureIcons.kt）、设置/首页/AI/合盘各入口补齐；南枫云小字精简及居中图标标题。
- 云同步ALREADY_CURRENT不更新时间的显示问题修复：lastCheckedAt可选且持久化，只在真正确认云端后更新，lastSyncedAt保留上传/恢复含义。实际账号漏传UNKNOWN；只读取到周期Worker注册且等下次执行，之后设备断开。
- 331项App测试0失败/0错误/1跳过；Debug/Release构建与Lint通过。截图、候选哈希及边界见ui-refresh-20261021.md末节。
- 新候选原签名Release在/tmp/bazi-ui-cloud-release-20261022/，未覆盖手机/未发布；先前下面记录的b9b374候选是本轮图标修复之前已安装版本。不要混淆。
- 工作区未提交，保留无关文档WIP；未触发真实同步、主设备UI自动化或connected测试。


## 2026-10-22 当前界面增量覆盖安装

- assembleRelease + lintRelease PASS，复用此前同源码定向207项测试证据。
- OPPO PKH120（3B157F009E800000）：原签名、非debuggable Release 1.0.16(10048)，pm install -r --user 0 返回Success；未启动或自动化操作App。
- 安装前后appId、CE/DE数据目录inode、目录路径和首次安装时间一致；安装后拉回APK SHA256与冻结候选一致。未逐条验证业务数据或真实交互。
- 原证书SHA256：0f89bc92cb127895e6881cda9d3c3c641e0efc0f39a9728eedf2585f8e12fdf3。沿用项目既有身份。
- 冻结APK：/tmp/bazi-overlay-20261022/NanfengBazi-Android-v1.0.16-release.apk；SHA256：b9b37414be434eabe595998862bec72c6cb75019eec4995920de567c9f4ef60f。
- 构建、安装、前后元数据及验签记录：/tmp/bazi-overlay-20261022/。当前工作区未提交，本轮未上传GitHub。

## 2026-10-22 最新增量交接

- 当前任务完成源码修复：默认星座徽标46→36.8dp；保存/编辑摘要保留星座；目录在IO补齐旧投影缺字段，页面同步发布结果。
- 新文件：core/engine-tyme/src/main/kotlin/com/nanzhufeng/nanfengbazi/engine/tyme/TymeWesternZodiacResolver.kt、app/src/test/kotlin/com/nanzhufeng/nanfengbazi/CaseCatalogZodiacTest.kt。
- 定向207测试及assembleDebug PASS，日志 /tmp/bazi-zodiac-final-20261022.log；未设备验收/覆盖安装/发布。未提交，保留此前UI工作区及无关文档变动。后续若交付须同签名正式包保数据覆盖，不能使用本轮Debug包到主设备。

# 当前交接：南枫记设计语言适配

更新日期：2026-10-21。当前修改范围与验证边界见[界面适配记录](ui-refresh-20261021.md)。

- 用户最终选择：只改应用内图标，保留启动图标；白色底部导航、实心黑色图标、橙色选中，参考用户图片。
- 本轮修改共享主题、导航图标、各页标题/返回、设置语义图标与共享选项；领域算法、仓储、缓存调度不变。
- 先前未提交的可迁移经验/流畅度文档及next-codex-prompt删除保留，不能纳入无关提交。
- 未正式签名发布或覆盖主设备。全皮肤/宽屏/大字体的完整视觉矩阵仍待验证。

以下为前次合同checkpoint的历史交接，不代表本轮工作区完成或已提交。

# 当前交接：合同复查与checkpoint

更新日期：2026-10-12。整体状态见[PROJECT_STATUS](PROJECT_STATUS.md)，现行规则从[CONTRACT_INDEX](CONTRACT_INDEX.md)进入。

## 本轮范围与完成项

- 基线e48996b；既有上一轮复盘WIP保留。本轮未改业务代码、测试、依赖、构建或设备配置。
- 当前需求/架构/测试/差距审计重新整理；五份完整原文归档历史。领域数据恢复细则移至DATA唯一正文；云/样本/第三方/图标与根AGENTS/Skill路由同步。
- 15类冲突/维护风险与源码依据见[统一记录](verification/contract-consolidation-20261012.md)，机械检查/反例与原文字节哈希见[机器报告](verification/contract-consolidation-20261012.json)。
- 新增复查C16–C19：检查器由索引类型列确定范围，校验章节锚点/多行命令/必需Skill和非空UI元数据；9项回归及4项旧隔离测试通过。报告见[checkpoint验证](verification/checkpoint-contracts-20261012.json)。
- scripts/check_contract_consistency.py只读检查现行合同，排除历史记录。它通过不等于业务/性能完全验收。
- 本轮未重跑Gradle或设备/真实服务，1020项JVM/0失败/2跳过及Lint零错误保留2026-10-11原证据时间。

## 保留与后续

- 本轮本地checkpoint-contracts-20261012纳入已核查文档/Skill/维护脚本；未推送。既有docs/next-codex-prompt.md删除保持未恢复/未暂存。精确增量回滚快照位于/tmp/bazi-contracts-20261012-backup/，永久历史正文位于docs/history/contracts/。
- 未重建已冻结APK、未改变原签名/现有功能，也没有新增个人Skill或长期记忆。
- archivesName命名差异、PROVISIONAL边界、真实问真/AI/Google/Supabase/跨设备恢复和全面目标设备性能继续单列；后续按明确反馈处理。
