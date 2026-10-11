# Project Status

> 当前状态摘要；代码与适用验证证据优先。CURRENT_HANDOFF.md保存任务接续细节。

## Verified

- Last verified: 2026-10-11 13:24 +08:00
- Verified branch: main
- Code baseline: c2c4da75a1cb92a518f4819abfd5c897e97dcf12
- Verification scope: WORKTREE；生产源码与Code baseline一致，新增fresh_jvm_tests.init.gradle与本轮实跑临时副本字节一致；合同／经验文档增量与永久报告随本地checkpoint冻结。
- Verified by: Codex
- Status: Active

## Current

- Android 1.0.16(10048) Release已生成，沿用项目既有签名，非DEBUGGABLE；OPPO同签名保数据覆盖Success。
- 节令流月／流日／流时边界、23点次日默认口径、专业细盘交互与记录冷启动优化已实现并有本地测试／隔离验收；80%记录四柱预览已进入该安装包。
- GitHub v1.0.16正式Release已发布为Latest，附件为APK与SHA256SUMS.txt；完整下载读回哈希与已覆盖手机的冻结包一致。
- README展示当前最终APK的API35模拟器1080×2400页面，使用合成资料；远端渲染及三张截图Git对象均核对一致。

## In Progress

- 无；本轮回归与增量正式沉淀已完成，随本地checkpoint冻结。

## Blocked

- 无。此前Git HTTPS推送超时已通过临时HTTP/1.1配置重试解决。

## Tests

| 检查 | 结论 | 时间／范围 | 命令或证据 |
| --- | --- | --- | --- |
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

已知失败：无本轮未解决的自动测试失败；相关外部包对账跳过1项。

## Verification Gaps

- 未验证全部OPPO交互、真实AI／Google登录／云同步以及真实问真包对账。
- 模拟器帧采样不能替代真机响应时延；首次渲染仍有慢帧，不承诺所有设备零延迟。

## Sync & Safety

- Sync checked at: 2026-10-11 13:24 +08:00；checkpoint提交之前的观察值。
- Code / docs sync: OK；当前版本、签名范围、正式发布及README预览一致。
- Checkpoint scope: 本轮合同、文档和fresh JVM执行脚本纳入本地checkpoint-v1.0.16-20261011-final；既有docs/next-codex-prompt.md删除保持未暂存。
- Remote sync: LOCAL ONLY（本轮checkpoint增量）；Code baseline和v1.0.16已推送并发布，本轮只建本地checkpoint，不改变已发布APK。
- Rollback point: 2d5871f57fe20f2e6753a093f17d2c9c6a7e389f；本地checkpoint-v1.0.16-20261011-pre-consolidation标签保留当前已验证代码与原文档；不代表设备降级验收。
- Secrets/config risk: NONE（本次新增暂存内容扫描范围）；密钥、签名配置、APK和临时验收文件不入Git。

## Next

1. 根据用户下一轮反馈开展定向验证；真机业务交互与真实服务证据单列。
