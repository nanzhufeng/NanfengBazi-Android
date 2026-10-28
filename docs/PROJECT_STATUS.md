# Project Status

> 当前状态摘要；代码与适用验证证据优先。CURRENT_HANDOFF.md保存任务接续细节。

## Verified

- Last verified: 2026-10-28T12:45:13+08:00；年柱/属相提示正式Release构建、同签名保数据覆盖及APK回读通过；11:05本地423项JVM（422通过、1既有跳过）、Debug构建、Release Kotlin编译和两变体Lint证据保留原时间。
- Verified branch: main
- Code baseline: 226e5449f6ff5cc18279a0f5511c78a0c79294a7
- Verification scope: COMMIT（当前生产源码）；193项实际生产源码/配置与Git及12:45已覆盖的Release一致。JVM/Lint和覆盖证据保留各自原时间，渲染/真实服务缺口仍在；[checkpoint核验](verification/checkpoint-20261028.json)。
- Verified by: Codex
- Status: Active

## Current

- 2026-10-28当前代码checkpoint `226e5449f6ff5cc18279a0f5511c78a0c79294a7` 已建立；相关核心新增文件全部纳入Git，详见[核验](verification/checkpoint-20261028.json)。

- 2026-10-28：移除通用“历史快照”提示；后台只读复用引擎年界核对，只有采用年柱或已有属相确认不符才提示具体差异。正常旧盘/缺失证据不提示，原快照不改写。已于2026-10-28T12:45:13+08:00同签名非debuggable Release保数据覆盖OPPO，数据目录标识/首次安装保持，APK回读哈希一致；[验收](verification/year-zodiac-notice-20261028.json)。

- 2026-10-26本轮年界修复已同签名Release保数据覆盖OPPO；安装Success，数据目录标识/首次安装不变，实际APK回读哈希7bee23ae…一致，真实业务验证NOT RUN。覆盖前“未安装”状态以下作历史记录。

- 2026-10-26：立春年／月边界全链路已按方案校准。北京时间2000-02-04 08:00为己卯／丁丑／兔，20:41为庚辰／戊寅／龙；交节分钟／时辰不确定时明确候选、禁止唯一采用。万年历不再丢分秒，海外/历史offset进入同一Instant比较。旧盘保留，自动资料库维护不跨规则版本替换采用盘。证据见[专项验收](verification/lichun-year-boundary-20261026.json)。
- 主设备仅在收尾设备清单中再次可见，未读取真实命例、未覆盖安装；上次10月25日fca73621…安装回执为历史，不代表本轮年柱版本已装。

以下为之前阶段的历史记录，不代表本轮候选安装状态：

- 2026-10-25：详情身份栏漏用星座补齐规则已修复；目录和详情共用CaseZodiacPresentation，相关213项测试与Debug构建、三详情入口模拟器核对通过。此修复已同签名Release保数据覆盖OPPO并回读哈希一致（fca73621…），见[验收记录](verification/case-identity-zodiac-20261025.json)。

- 最新视觉增量：模型设置输入框/选择框/按钮白底、关于卡片无阴影外边框；58项定向测试、Debug/Release构建与Release Lint通过；已同签名保数据覆盖OPPO，现装哈希56997e7e…，回读一致。以下57e553a0…为上一轮历史包。

- 2026-10-22 WORKTREE：共享功能图标无装饰底块，首页/设置/AI/合盘入口补齐；南枫云文案精简、最近检查时间与备份时间分开记录。331项App测试0失败/0错误/1跳过，Debug/Release构建与Lint通过。
- 当前候选原签名非debuggable Release 1.0.16(10048)，哈希57e553a0…，已保数据覆盖OPPO并回读哈希一致，未发布。原手机10月22日已覆盖的b9b374…包不包含本次图标补齐和云页面增量。精确证据见[界面适配记录](ui-refresh-20261021.md)。
- 真实账号是否漏传仍UNKNOWN；只确认周期任务已注册，之后手机断开。时间停留11号不能单独证明上传失败，当前源码已补成功检查时间。
- GitHub公开v1.0.16及原10月11日验收为历史冻结交付，不能用于证明本轮候选安装/发布或云服务恢复。

## In Progress

- 年柱专项代码、本地回归与构建已完成；真实历史命例影响扫描及主设备验收未执行。真实账号同步仍UNKNOWN，完整皮肤/宽屏/大字体矩阵未穷尽。

## Blocked

- 无。此前Git HTTPS推送超时已通过临时HTTP/1.1配置重试解决。

## Tests

| 检查 | 结论 | 时间／范围 | 命令或证据 |
| --- | --- | --- | --- |
| 年柱/属相提示 | PASS | 2026-10-28；12项专项红绿回归；App 353项/引擎70项，合计0失败/错误、1既有跳过；Debug构建、Release Kotlin编译、两变体Lint | [专项验收](verification/year-zodiac-notice-20261028.json) |
| 立春年界全链路 | PASS | 2026-10-26；1063项JVM、0失败/错误、2原有跳过；Debug/Release构建；两套Lint各0错误/44警告/2信息 | [专项验收](verification/lichun-year-boundary-20261026.json) |
| 界面适配Debug门禁 | PASS | 2026-10-21；App 327项、0失败/错误、1跳过；Lint 0错误/43警告/2信息；assembleDebug | [界面记录](ui-refresh-20261021.md) |
| checkpoint合同复查/维护回归 | PASS | 2026-10-12 12:42 +08:00；18个现行/审计/Skill、9项新回归及4项既有隔离测试 | [Checkpoint报告](verification/checkpoint-contracts-20261012.json) |
| 现行合同统一与漂移检查 | PASS | 2026-10-12；17个合同/Skill、历史原文字节核验、Schema/危险指令/断链反例验证 | [合同统一报告](verification/contract-consolidation-20261012.json) |
| 项目完整复盘文档/Skill | PASS | 2026-10-11 14:34 +08:00；全量清点/关键链路复核、5个Skill格式校验、链接/JSON/差异检查 | [复盘清单](verification/project-retrospective-20261011.json)、完整开发档案 |
| 全模块最终JVM回归 | PASS | 2026-10-11 13:08 +08:00；实际重跑1020项，0失败／错误、2跳过 | [永久验收报告](verification/checkpoint-20261011.json)，test＋fresh_jvm_tests.init.gradle |
| 两变体最终Lint | PASS | 2026-10-11；各0错误、50警告、2信息 | lintDebug＋lintRelease |
| 增量经验审计 | PASS | 2026-10-11；11个本地链接、无占位／疑似秘密 | app-experience-audit.md与project-context.md校验 |
| Release构建 | PASS | 2026-10-11；当前源码 | :app:assembleRelease；本地最终门禁日志 |
| Release单测 | PASS | 2026-10-11；327项，0失败／错误，跳过1真实问真包对账 | :app:testReleaseUnitTest |
| Release Lint | PASS | 2026-10-11；零错误，既有警告仍存在 | :app:lintRelease |
| 80%视觉改动Debug门禁 | PASS | 2026-10-11；58项交互合同与构建 | :app:assembleDebug、相关InteractionShapeContractTest |
| 隔离模拟器 | PASS | API35；细盘连续点选、滚动、标签、长图与宽窄恢复；最后配置升级包另复核关键流程 | interaction-performance-audit.md；本地验收记录未上传 |
| 主设备覆盖 | PASS | 2026-10-11 11:55 +08:00；1.0.16精确APK | 同证书，UID、首次安装、CE/DE数据目录inode保留，APK读回哈希一致 |
| GitHub附件回读 | PASS | 2026-10-11 12:45 +08:00；70604319字节APK和98字节校验文件 | [v1.0.16 Release](https://github.com/nanzhufeng/NanfengBazi-Android/releases/tag/v1.0.16) |
| 真实AI／云服务 | NOT RUN | 本轮未触发外部服务 | — |

已知失败：无本轮未解决的自动测试失败；同一外部包对账在两App变体各跳过1项。

## Verification Gaps

- 2026-10-28提示专项：同签名正式覆盖及数据目录元数据核验PASS；模拟器/主设备渲染与真实命例验收NOT RUN，未读取或逐条核对业务库。

- 新增秒选择器界面验收NOT VERIFIED：独立API35模拟器进程启动超时，无业务异常栈，已清理；不能由构建/单测推断渲染通过。
- 年界：1901—2100逐年边界/独立模型对账已执行，1950—2050差异<5秒；2051—2100最大约92.52秒，不宣称未来秒级权威精度。主设备历史采用盘未扫描/自动改写。
- 新增年界证据的Room/云结构化无附件样本往返通过；另发现既有导入来源笔记/事件残留附件引用可使云恢复被拒绝，本轮未改云算法，见专项报告仍需确认。

- 本轮仅核对专用模拟器代表页，未完整验证六皮肤/大字体/宽屏矩阵；Debug慢帧不能据此判定性能改善。

- 未验证全部OPPO交互、真实AI／Google登录／云同步以及真实问真包对账。
- 模拟器帧采样不能替代真机响应时延；首次渲染仍有慢帧，不承诺所有设备零延迟。

## Sync & Safety

- Sync checked at: 2026-10-28T12:50:56+08:00；本地代码checkpoint/文档/合同与冻结安装证据核验；本阶段未新增构建或设备操作，未刷新远端。
- Code / docs sync: OK（当前checkpoint范围）；领域/数据/云/测试合同与相关档案同步，既有未验证项不升级为通过。
- Checkpoint scope: 当前app/core源码、测试、参考CSV与校验脚本54文件（含16新增）已纳入 226e5449f6ff5cc18279a0f5511c78a0c79294a7；相关文档单独提交。三项既有文档WIP保留，见[核验](verification/checkpoint-20261028.json)。
- Remote sync: LOCAL ONLY；本轮checkpoint未推送/发布，GitHub v1.0.16沿用此前冻结交付。
- Rollback point: e48996b6071f0c2f6f4dad99331eed475544e69c，本地checkpoint-v1.0.16-20261011-final；本轮归属文件另有精确备份，不代表设备降级验收。
- Secrets/config risk: 本轮未读取/修改私有签名配置、凭据或主设备业务数据；未将APK或真实命例纳入Git。

## Next

1. 根据用户下一轮反馈开展定向验证；真机业务交互与真实服务证据单列。
