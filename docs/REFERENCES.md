# 第三方参考与采用边界

> 当前采用范围以模块构建文件与调用方为准，路由见[合同索引](CONTRACT_INDEX.md)。上游链接保留为历史来源，本轮未联网重新核验。

## 已采用

### 6tail/tyme4j

- 地址：https://github.com/6tail/tyme4j
- 晚子时官方实现：
  https://github.com/6tail/tyme4j/blob/master/src/main/java/com/tyme/eightchar/provider/impl/LunarSect2EightCharProvider.java
- 官方文档：https://6tail.cn/tyme.html
- 用途：Stage 0 的八字、节气、起运和大运计算适配器；Stage 4A 依据官方
  `LunarHour.fromYmdHms`、闰月负月份约定和 `getSolarTime` 接通农历转换，并从最终
  `EightChar` 固化十神、藏干、十二长生、旬空、纳音、生肖、星座与相邻节气；公开测试
  用作首批黄金样本。童限结束时刻按官方定义作为开始起运的精确公历时间，当前十二步/120年大运
  读取其干支、起止年龄和起止年份。Stage 7B 依据官方 `LunarSect2EightCharProvider`
  “晚子时日柱算当天”语义接入第二种子时口径。
- 采用边界：第三方类型不进入 `core:domain`；全局起运 provider 必须加锁、临时切换并
  恢复；正向八字provider按调用显式选择，不改写全局；四柱反查的受锁临时切换/finally恢复例外见TymeFourPillarsLookup。

### klausbrunner/solarpositioning

- 地址：https://github.com/klausbrunner/solarpositioning
- 上游算法：https://midcdmz.nrel.gov/spa/
- 用途：Stage 4A 真太阳时组件使用 2.0.12 的 NREL SPA 实现计算太阳中天时刻，并据此
  分离经度平太阳时校正与均时差校正；依赖要求 Java 17、无额外运行时依赖、MIT。
- 采用边界：第三方类型只存在于 `core:solar-time`；领域和快照只保存本项目定义的
  `TrueSolarTimeEvidence`。算法支持范围固定到 1..6000 年，升级必须更新版本与黄金样本。

### android/nowinandroid

- 地址：https://github.com/android/nowinandroid
- 用途：参考 Android 官方样板的模块化、依赖单向和测试分层思想。
- 采用边界：本项目规模尚小，不复制其完整多模块复杂度；只保留领域端口和引擎适配器分离。

## 已用于测试对照（非生产依赖）

### 6tail/lunar-java

- 地址：https://github.com/6tail/lunar-java
- 用途：当前engine-tyme/test依赖lunar 1.7.7，作为另一代实现的差分对照；不是待接入的生产引擎。
- 限制：与 Tyme4j 同源，不能被当作真正独立裁判。

## 仅供后续方法参考

### sxwnl/sxwnl-cpp

- 地址：https://github.com/sxwnl/sxwnl-cpp
- 用途：后续针对节气瞬间、儒略日和历法边界做独立天文层交叉检查。
- 限制：语言和领域 API 不同，不直接引入 Android 生产依赖。

### RedSC1/bazi_core

- 地址：https://github.com/RedSC1/bazi_core
- 用途：仅参考其“绝对时刻计算年/月、真太阳时计算日/时”的参数分离方式，并用于核对
  本项目的作用规则是否有公开实现先例。
- 限制：该项目底层依赖明确声明 AI 辅助、精度不保证且存在算法商业授权提醒，因此不作为
  生产依赖或算法真值。

### NOAA Solar Calculation Details

- 地址：https://gml.noaa.gov/grad/solcalc/solareqns.PDF
- 用途：公开核对均时差、经度和时区组成真太阳时偏移量的公式关系。
- 限制：当前生产实现使用 NREL SPA 中天结果，不直接复制 NOAA 近似公式代码。

## 不采用

- 不复制开源“算命 App”的业务结论、断语或案例模型；
- 不以单个库的默认值替代本项目显式计算口径；
- 不把 GitHub 示例通过测试等同于“问真输出已经一致”。
