# Stage 1 数据与备份合同

## 命例真值

`BaziCase` 是案例数据聚合，包含：

- 稳定 ID、别名、姓名状态、性别和来源类型；
- 当前采用的结构化 `BirthInput`、多个出生时间候选与个人资料字段；
- 版本化计算快照；
- 命主反馈、师傅点评、笔记和分析原文；
- 事件时间线；
- 来源附件与逐字段证据；
- 分组和标签；
- 创建时间、更新时间和修订号。

姓名和职业等可清空文本使用三态：

| 状态 | 含义 | 协议值 |
|---|---|---|
| `ABSENT` | 来源没有提供 | `value = null` |
| `PRESENT` | 有明确非空值 | `value = 文本` |
| `CLEARED` | 用户明确清空 | `value = null` |

禁止用空字符串把三种状态合并。

## 写入合同

- 所有增量写入经过 `CaseRepository.save`。
- 新建成功后修订号为 1。
- 已有 ID 且未携带期望修订号，返回 `AlreadyExists`。
- 期望修订号与当前不一致，返回 `RevisionConflict`。
- 更新在单个 Room 事务中替换聚合子项，失败不产生半条命例。
- 列表顺序字段独立持久化，不能依赖时间戳或随机 ID 排序。

## Room Schema

- 当前版本：6。
- Schema 文件提交在 `core/data/schemas/`。
- v1→v2 增加来源类型和子记录顺序，旧命例默认来源为 `MANUAL`。
- v2→v3 增加收藏、置顶和最近查看；v3→v4 增加软删除与复制来源；v4→v5
  增加记录/事件历史；v5→v6 增加出生时间候选 JSON，旧命例默认空候选。
- 禁止 destructive migration。

## 备份合同

备份格式版本：1。ZIP 包含：

```text
manifest.json
cases.json
groups.json
tags.json
snapshots.json
notes.json
analysis.json
events.json
settings.json
imports.json
attachments/
```

`manifest.json` 记录 App、Schema、引擎、规则、数量，以及每个数据文件和附件的
相对路径、字节数、SHA-256。

当前恢复限制：

- 只允许空数据库和空附件目录；
- 不接受绝对路径、反斜杠、冒号、空路径段、`.` 或 `..`；
- 限制条目数、单文件大小、JSON 大小和总展开大小；
- 文件集合、哈希、数量和外键引用必须全部一致；
- 附件提交失败后清除本次数据库写入；
- 非空库已支持零写入逐命例冲突候选预演；逐例决策提交、密码备份恢复和进程崩溃中断
  恢复尚未实现。
- 恢复计划中的每个来源命例必须恰有一个显式决策；计划生成前重新计算冲突候选，任何
  revision、回收站状态或候选变化都使预览过期。计划不会自行执行数据库或附件写入。
