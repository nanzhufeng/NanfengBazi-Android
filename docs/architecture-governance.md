# 架构与治理

## 业务真相与唯一入口

本阶段唯一业务真相是：**同一出生输入和同一计算口径，必须得到可版本追踪、可重复验证的计算结果。**

所有计算必须经过：

```text
调用方 → BaziEngine.calculate(input, profile) → 引擎适配器 → 版本化结果
```

任何界面、导入器或数据库代码都不得绕过该入口直接调用 Tyme4j。

## 所有权矩阵

| 概念 | 唯一所有者 | 当前消费者 | 禁止事项 |
|---|---|---|---|
| `BirthInput` | `core:domain` | 引擎适配器 | 用 UI 字符串代替结构化时间 |
| `CalculationProfile` | `core:domain` | 引擎适配器 | 隐式更改换年或起运口径 |
| `CalculationResult` | `core:domain` | 测试；后续仓储 | 只保存展示文本、不保存版本 |
| `BaziEngine` | `core:domain` | 后续用例层 | 多套平行计算入口 |
| Tyme4j 状态隔离 | `core:engine-tyme` | `TymeBaziEngine` | 其他模块访问全局 provider |

## 模块边界

- `app`：当前只有占位界面，不承载业务计算。
- `core:domain`：纯 Kotlin 领域模型和端口，不依赖 Android 或第三方历法库。
- `core:engine-tyme`：Tyme4j 适配器及状态隔离。
- 后续 OCR、数据、备份模块必须在进入对应阶段后新增，不能提前塞入 `app`。

## 变更门禁

下列变更必须同步更新规则文档、黄金样本和测试：

- 年界、月界、子初换日、起运算法；
- 公历/农历/真太阳时归一化；
- 引擎版本或规则版本；
- 结果字段含义。

