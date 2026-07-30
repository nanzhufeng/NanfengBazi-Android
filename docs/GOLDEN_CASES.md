# 黄金样本

机器可读样本位于：

`core/engine-tyme/src/test/resources/golden-cases-v1.json`

每条样本至少包含：

- `id`：稳定标识；
- `source`：公开来源或人工边界说明；
- `input`：不含真实用户信息的结构化出生时间；
- `profile`：计算口径；
- `expected`：四柱或边界关系；
- `engineVersion` 与 `ruleVersion`。

当前公开回归样本取自 Tyme4j 自身公开测试；边界样本由立春节气 API 生成。后续接入问真
验收案例时，真实数据应保存在加密、本地、Git 忽略的验收夹具中，不能复制进仓库。

