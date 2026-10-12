---
name: nanfeng-bazi-code-review
description: Review scoped 南枫八字 diffs for domain correctness, data integrity, privacy and interaction regressions. Use for review requests, handoff or release audits; do not invent findings from missing evidence.
---

# 南枫八字代码审查

## 输入

先计数/读取限定diff统计，再看归属增量；获取受影响合同与测试，保留无关变化。所有者映射见 ../../../docs/南枫八字完整开发档案.md；数据、领域与云合同为专项依据。

## 审查顺序

1. 领域：是否复制计算；规则是否版本化；节令/23点/真太阳时/父区间候选是否统一；选择结果与重新定位是否一致。
2. 数据：caseId + revision、聚合事务、三态、历史记录/软删除和迁移；恢复预览零写入、文件再认证、显式计划与journal回滚；禁止destructive migration。
3. 状态/性能：缓存未知是否误判为空、维护/写盘是否阻塞首屏、过期协程是否覆盖新选择、是否整库/N+1；标签/宽窄/导出是否恢复案例视口。
4. 外部/隐私：AI发送范围/采用记录、Keystore加密、云的启用后后台语义和本机源、账号隔离、取消/重试；不输出秘密值或真实资料。
5. UI：导航/保存/返回真实接通，shape/clip/click一致，命中区域与尾部可达，内外屏分开；合盘/摘要文案须有实际领域信号支持。
6. 测试/交付：改动风险是否有相称行为证据；源码合同不是设备/性能证明；版本/archivesName/动态构建时间/签名与冻结APK需分别核对。connected任务和主设备门禁不可绕过。

## 输出与停止

只报告可定位、可解释影响的缺陷：优先级、文件/行或符号、触发条件、依据、建议与缺少的验证。无可证实缺陷则说明审查范围和余留风险，不为凑数量发明问题。审查请求默认不改业务代码；获得修复范围后再按development流程推进。
