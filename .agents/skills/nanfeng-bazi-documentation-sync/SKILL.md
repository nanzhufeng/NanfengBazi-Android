---
name: nanfeng-bazi-documentation-sync
description: Update 南枫八字 archives, durable rules, state and reusable experience from repository evidence. Use after verified increments or full retrospectives; keep historical and current verification separate.
---

# 南枫八字文档同步

## 输入与盘点

先从../../../docs/CONTRACT_INDEX.md定位唯一正文；读取根AGENTS、../../../docs/PROJECT_STATUS.md、CURRENT_HANDOFF.md；核对HEAD、工作区、配置、相关源码与测试报告。完整复盘先盘点所有跟踪文件/模块及全部refs历史，保存覆盖清单；大文件按符号复核，不以清单冒充逐行无缺陷审计。

## 执行

1. 先保存归属文档的可恢复快照。源码/验证优先，Git主题证明变更意图；历史聊天和旧交接只作线索，不补造开发过程或因果。
2. 唯一正文：AGENTS仅长期边界；架构/领域/数据/云合同拥有规则；完整开发档案记录设计/历史/冲突/证据；PROJECT_STATUS记录当前验证与同步快照；CURRENT_HANDOFF只记录任务接续。
3. 每项结论映射代码路径/符号、测试或可达commit；记录文档与代码冲突及处理。版本/schema/配置和生产注入必须以实际代码核对，不能只复制依赖表或旧能力清单。
4. 保留证据运行时间、代码基线和作用范围。仅文档变更可复核冻结报告，不把旧PASS写成新重跑；没有真实设备/服务证据写未确认。
5. 可迁移经验分通用原则、项目专属和不可照搬项，写触发条件/方法/验证/限制。五个项目Skill职责独立，先更新既有入口，不复制个人Skill正文，不自动新增长期记忆。
6. 运行python3 scripts/check_contract_consistency.py，修改检查器时运行scripts/test_check_contract_consistency.py的回归，再验证相对链接、引用路径/符号、JSON、Skill frontmatter/UI元数据、秘密模式和git diff --check；确认业务源码/构建配置保持原样。

## 完成、边界与回滚

列新增/修改文件、实跑或复核的验证、文档冲突和仍未确认的问题；动态状态不写AGENTS。文档任务不自动授权提交/推送/发布或设备服务操作；回滚使用本轮精确快照，不覆盖无关变化和原有删除。
