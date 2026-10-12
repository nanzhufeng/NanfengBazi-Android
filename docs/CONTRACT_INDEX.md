# 现行合同索引

> 2026-10-12统一；生产实现基线e48996b，功能与构建配置未改变。此索引拥有合同分类与读取路由，不重复维护业务规则。验证/当前同步状态见[PROJECT_STATUS](PROJECT_STATUS.md)，本轮冲突与处理见[统一记录](verification/contract-consolidation-20261012.md)。

## 唯一正文

| 合同/信息 | 正文 | 使用范围 | 类型 |
| --- | --- | --- | --- |
| 每次开发的安全与长期边界 | [根AGENTS](../AGENTS.md) | 当前用户指令优先；设备、资料、安全边界不能因旧实现放宽 | 现行 |
| 产品现有能力与非承诺 | [PRODUCT_REQUIREMENTS](PRODUCT_REQUIREMENTS.md) | 功能范围、入口、产品完成条件；未来计划另列 | 现行 |
| 所有者、依赖与跨层状态 | [architecture-governance](architecture-governance.md) | 唯一业务链路、例外、首帧/校准/导出状态 | 现行 |
| 时间、计算、资料保真与研究语义 | [domain-rules](domain-rules.md) | 默认23点次日且保留显式选择；算法/领域规则 | 现行 |
| 聚合、Schema、交换、备份与恢复 | [DATA_CONTRACT](DATA_CONTRACT.md) | 增量写入与数据层特权事务、预览/提交/回滚 | 现行 |
| 身份、云加密与后台生命周期 | [cloud-sync-contract](cloud-sync-contract.md) | 启用后的自动备份、本机源、空白设备恢复 | 现行 |
| 门禁与证据等级 | [TEST_STRATEGY](TEST_STRATEGY.md) | 什么要验证、怎样解释结果；不授予设备/外部操作授权 | 现行 |
| 算法冻结样本 | [GOLDEN_CASES](GOLDEN_CASES.md) | 合成/公开样本、来源与限制，不是全算法正确证明 | 现行 |
| 图标包装 | [app-icon-fidelity-standard](app-icon-fidelity-standard.md) | 当前母版与包装约束，视觉和设备证据另列 | 现行 |
| 第三方算法采用边界 | [REFERENCES](REFERENCES.md) | 采用/仅对照/不采用，不证明外部链接今天仍可用 | 现行 |
| 当前能力与缺口 | [REQUIREMENT_GAP_AUDIT](REQUIREMENT_GAP_AUDIT.md)、[PROJECT_STATUS](PROJECT_STATUS.md) | 审计与动态事实，不能作为第二套规则 | 审计 |
| 决策与历史经验 | [decision-log](decision-log.md)、[开发档案](南枫八字完整开发档案.md)、[经验](可迁移开发经验.md) | 保留原因和演变；冲突时回到以上现行所有者 | 历史 |
| 临时接续 | [CURRENT_HANDOFF](CURRENT_HANDOFF.md) | WIP与下一步，不发布永久规则 | 接续 |
| 项目开发工作流 | [开发](../.agents/skills/nanfeng-bazi-development/SKILL.md)、[排错](../.agents/skills/nanfeng-bazi-debugging/SKILL.md)、[测试](../.agents/skills/nanfeng-bazi-testing/SKILL.md)、[审查](../.agents/skills/nanfeng-bazi-code-review/SKILL.md)、[文档同步](../.agents/skills/nanfeng-bazi-documentation-sync/SKILL.md) | 可重复执行流程；主题规则仍由上述所有者拥有 | 现行 |

## 冲突与变更流程

1. 工程事实按当前源码/配置和适用验证核对；不能把代码存在写成真实服务已验收。当前用户指令与安全边界不由旧代码或历史脚本覆盖。
2. 明确概念所有者后只改其正文；AGENTS、Skill和其他文档留路由及必要硬边界，不复制完整合同。需求不再把已实现能力写成未来功能，历史PASS不刷新运行时间。
3. 行为改动须同时审查相关用例、数据/版本、入口与测试；纯合同整理保持生产代码/配置不变。不能为了“以实现为准”静默认可违反用户约束的实现。
4. 新增合同主题时在上表登记正文与类型；检查器自动读取现行/审计行及本索引，不再维护第二份文件清单。本地运行 `python3 scripts/check_contract_consistency.py`，再按TEST_STRATEGY选业务门禁。检查器只验证可机械核对的事实、命令、链接与章节锚点，语义仍需代码审查。
5. 明确记录冲突、依据、处理与剩余风险；新基线/发布事实放状态和验证报告，不写进长期AGENTS。

## 历史阶段资料

[归档目录](history/contracts/README.md)中的五份原阶段正文完整保留，只是历史证据，不属于现行合同集合；各现行文件均有对应历史链接。它们包含当时成立、后来被替代的限制/门禁，不可直接复制执行。

本地仪器/安装脚本的存在不解除根AGENTS设备门禁。全部connected任务始终禁止，主设备只按授权接收已验签同身份Release覆盖；脚本、旧截图、旧版本和旧云端结果均不能作为当前授权。
