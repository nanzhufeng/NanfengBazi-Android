---
name: nanfeng-bazi-development
description: Implement scoped 南枫八字 Android changes while preserving calculation, persistence and UI state ownership. Use for features or Kotlin/Compose/data changes; use the debugging skill for unresolved defects.
---

# 南枫八字开发

## 输入与读取

先读取根AGENTS、PROJECT_STATUS及任务相关合同。需求/架构/领域/数据事实分别从 ../../../docs/PRODUCT_REQUIREMENTS.md、architecture-governance.md、domain-rules.md、DATA_CONTRACT.md 获取；范围和历史见 ../../../docs/南枫八字完整开发档案.md。核对HEAD与未提交改动，保留无关WIP。

## 执行

1. 将用户目标映射到现有链路：AppContainer → 领域接口/数据服务 → 用例/StageTwoViewModel → Compose；明确输入、真实完成结果、失败/取消和受影响入口。
2. 算法使用BaziEngine/ProfessionalFortuneResolver，增量写入使用CaseRepository；备份/交换特权事务由既有数据服务执行，不从UI新增第二条管线。
3. 保持来源/计算/采用快照、caseId + revision及三态文本；记录、详情、返回和导出共用同一事实。目录投影先呈现再校准，写盘与维护不阻塞内存发布。
4. 按可验证的小增量实现。时间算法、迁移、恢复等先补能复现原问题的行为测试；可回滚低影响视觉调整用已有合同/目标渲染核验，不添加仅匹配实现的测试。
5. 专业视口按案例保存，标签/宽窄切换保持观察时刻；导出成功/失败/取消恢复原现场。缩小组件时同步核对clip、反馈和命中区域。
6. 根据testing Skill选择门禁；通过后同步状态、决策和档案对应段，不把自动测试当目标设备/服务验收。

## 停止、完成与回滚

需求的关键规则无事实依据时先明确缺口，继续独立工作；涉及真实服务、主设备或破坏性操作按根合同授权。完成须有真实调用链、相关门禁、变更清单与剩余边界。回滚只撤销本轮归属增量，不reset/clean无关工作区。
