# 当前能力与验证缺口审计

> 2026-10-12按当前e48996b生产实现复核；本文是事实审计，不是第二套产品/安全合同。现行正文见[合同索引](CONTRACT_INDEX.md)，动态验证见[PROJECT_STATUS](PROJECT_STATUS.md)。原alpha75完成表及阶段计划完整保留于[历史审计](history/contracts/REQUIREMENT_GAP_AUDIT-before-20261012.md)。

| 能力 | 当前代码依据 | 证据边界/仍需验证 |
| --- | --- | --- |
| 本地核心与可选联网 | Manifest有INTERNET；Tyme/Room本地、AI/云可选 | “无强制账号”成立，“应用无联网权限”不成立 |
| 时间/四柱/专业流运 | BaziEngine、ProfessionalFortuneResolver、版本化CalculationProfile | 真实问真对账/真太阳暂定口径外部边界仍缺 |
| 记录/分类/删除 | CaseRepository/Management、案例库、置顶/回收站/永久删除入口 | 全面目标设备路径不因源码存在而完成 |
| 详情/首帧/目录恢复 | StageTwoViewModel、CaseCatalogStore、saveable专业轴 | 旧回归/隔离性能证据适用范围见报告；仍有首帧慢帧 |
| 截图导入 | 生产容器注入AiVisionScreenshotOcrEngine，MLKit实现在core保留 | 历史离线fixture不证明当前AI服务实测/全来源兼容 |
| 数据与迁移 | Schema12、完整迁移链、已提交schema2–12 | v1测试构造不是所有历史用户库验证；禁止破坏迁移 |
| 恢复/交换/导出 | 显式计划、密码、附件事务/journal、真实页面捕获 | 已有本地行为/历史隔离证据，真实用户非空库/全部崩溃窗口另验 |
| 合盘/研究/笔记 | domain分析器、采用快照、本地历史与笔记版本 | 领域样本扩大与真实阅读路径继续按需验证 |
| AI/云 | 主动点评/视觉、Keystore加密、启用后云Worker | 真实API/Google/RLS/RPC/跨设备恢复尚缺，不写服务已验收 |
| Android交付 | non-debuggable原身份Release已有历史覆盖和附件回读 | 本轮未重新核验远端/设备，版本名称配置差异已记录 |

## 有依据的后续工作

1. 根据用户实际反馈补定向性能/业务路径或外部样本证据，不以旧阶段未做表盲目扩功能。
2. 下一次实际构建时处理archivesName落后内部版本的配置问题；本轮保持功能/构建代码不变。
3. 继续维护唯一所有者、迁移/状态/取消/导出回归和合同检查；真实服务/资料/主设备遵从独立授权边界。

在线精确地理编码、多引擎裁决、全部截图兼容等非承诺项以PRODUCT_REQUIREMENTS/domain-rules为准，不在本页重复建立新路线或永久规则。
