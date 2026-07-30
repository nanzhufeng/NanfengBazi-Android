# GitHub 参考与采用边界

## 已采用

### 6tail/tyme4j

- 地址：https://github.com/6tail/tyme4j
- 官方文档：https://6tail.cn/tyme.html
- 用途：Stage 0 的八字、节气、起运和大运计算适配器；Stage 4A 依据官方
  `LunarHour.fromYmdHms`、闰月负月份约定和 `getSolarTime` 接通农历转换；公开测试
  用作首批黄金样本。
- 采用边界：第三方类型不进入 `core:domain`；全局起运 provider 必须加锁、临时切换并恢复。

### android/nowinandroid

- 地址：https://github.com/android/nowinandroid
- 用途：参考 Android 官方样板的模块化、依赖单向和测试分层思想。
- 采用边界：本项目规模尚小，不复制其完整多模块复杂度；只保留领域端口和引擎适配器分离。

## 后续交叉验证候选

### 6tail/lunar-java

- 地址：https://github.com/6tail/lunar-java
- 用途：作为同一作者另一代实现的迁移差异参考。
- 限制：与 Tyme4j 同源，不能被当作真正独立裁判。

### sxwnl/sxwnl-cpp

- 地址：https://github.com/sxwnl/sxwnl-cpp
- 用途：后续针对节气瞬间、儒略日和历法边界做独立天文层交叉检查。
- 限制：语言和领域 API 不同，不直接引入 Android 生产依赖。

## 不采用

- 不复制开源“算命 App”的业务结论、断语或案例模型；
- 不以单个库的默认值替代本项目显式计算口径；
- 不把 GitHub 示例通过测试等同于“问真输出已经一致”。
