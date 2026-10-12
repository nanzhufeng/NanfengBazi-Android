---
name: nanfeng-bazi-debugging
description: Diagnose reproducible 南枫八字 defects and performance delays through state, time rules and data flows. Use for wrong results, missing records, import/restore failures or interaction regressions.
---

# 南枫八字排错

## 输入与事实源

读取根AGENTS、../../../docs/PROJECT_STATUS.md及相关合同。历史问题索引在 ../../../docs/南枫八字完整开发档案.md，性能证据在 ../../../docs/interaction-performance-audit.md；旧报告只作线索。

## 执行

1. 定义可判定红灯：操作、输入、一个可观察结果和必要阈值；注明冷/热入口、窗口布局、案例身份与证据层。先用合成资料安全复现。
2. 沿真实调用追踪首个分叉。排盘查BirthInput/CalculationProfile、交节/23点/真太阳时与候选父区间；数据查caseId、revision、聚合子项、仓储；导入查原图坐标/来源证据/采用值。
3. “数据消失”先区分未恢复投影、缓存未知和仓储事实。卡顿先定位主线程计算、整库/N+1、重复校准、维护锁/写盘和整页组装，不以去掉加载文字代替修复。
4. 快速点击/标签/返回查最新请求守卫、取消传播、案例观察时刻与saveable轴状态；导出查finally恢复。布局查真实约束、insets只计一次及内外屏分支。
5. 备份失败查路径/容量/清单/哈希、显式决策是否过期、事务/journal阶段；云查身份、恢复码、非空本机源与Worker代次，禁止触碰真实资料猜测原因。
6. 找到有证据的根因后修首个分叉，重跑同一红灯及最小回归。使用性能采样脚本前查看参数和设备保护；帧统计/可访问性空闲耗时不能写作点击延迟。

## 输出与边界

报告原红灯、根因路径/符号、修复、同信号结果及尚未验证环境。不跑connected任务；不在主设备装Debug/测试包或直读写数据库。无法复现时记录UNKNOWN与缺少的输入，不编造根因；修复回滚保持用户数据和无关WIP。
