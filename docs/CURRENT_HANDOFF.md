# 当前交接：v1.0.16 checkpoint与增量正式沉淀

更新日期：2026-10-11。全项目状态以[PROJECT_STATUS.md](PROJECT_STATUS.md)为准；本文件只保留当前任务接续事实。

## 当前基准

- 生产代码基线2d5871f57fe20f2e6753a093f17d2c9c6a7e389f；核心源码与发布标签v1.0.16的c2c4da75a1cb92a518f4819abfd5c897e97dcf12一致。本轮不改生产代码。
- 已发布并覆盖的APK SHA256：3a5f9f37a56e85ae13416300991bb4936c98595bb58966e50547f010a6f4fcae，1.0.16(10048)、非Debug、原安装身份。安装／发布回读已完成，本轮复核文件未变化，不重新生成、安装或发布。
- 本地checkpoint-v1.0.16-20261011-pre-consolidation标签保留本轮开始前的代码与文档。旧长交接仍可从[该提交的历史文件](https://github.com/nanzhufeng/NanfengBazi-Android/blob/2d5871f57fe20f2e6753a093f17d2c9c6a7e389f/docs/CURRENT_HANDOFF.md)按需检索，不再把历史阻塞误作当前问题。

## 本轮增量与验证

- 更新项目AGENTS：状态文档路由、目录恢复／维护／写盘分离、案例级视口与导出恢复、指标边界、原签名更新和冻结产物复用合同。
- 已有经验审计、context、可迁移经验与开发档案同步当前基准；既有八月经验标为历史，未重复沉淀。全模块实际重跑1020项，0失败／错误、同一真实问真包对账在两变体各跳过1项；两变体Lint零错误、各50警告／2信息。
- 永久结果与命令见[checkpoint验收报告](verification/checkpoint-20261011.json)。新增scripts/fresh_jvm_tests.init.gradle与实际执行副本字节一致，只强制JVM测试重跑，不选择connected任务、不重新打包。
- 原有核心新增文件已在Git；本轮新执行脚本和永久报告随checkpoint纳入Git。密钥、私有元数据、原始trace、临时目录与字节码排除。
- 两个既有个人Skill的三份reference完成增量更新，拥有者分别为通用性能／交付与八字交互。它们位于项目Git之外，有精确备份和独立验证；本轮不新增长期记忆。

## 边界与接续

- 全面OPPO交互、真实AI／Google／云服务及真实问真对账仍未验证；文档或目录保留不能冒充业务验收。
- 既有docs/next-codex-prompt.md删除保持未暂存，禁止恢复／清理／擅自提交。除这项排除外，本轮只提交已核查增量。
- 本轮文档／Skill校验通过，本地checkpoint纳入已核查增量；后续只按新用户反馈继续，不因文档收尾再次生成已冻结APK。
