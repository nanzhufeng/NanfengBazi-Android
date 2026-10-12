---
name: nanfeng-bazi-testing
description: Select sufficient safe verification for 南枫八字 changes and classify existing evidence. Use for JVM/static gates, regression, frozen artifact checks and device/service acceptance planning.
---

# 南枫八字测试

## 读取与选择

先运行python3 scripts/check_contract_consistency.py核对合同；读取根AGENTS与 ../../../docs/TEST_STRATEGY.md。先确认HEAD、改动路径、依赖/构建变化和旧报告适用范围，再区分静态、JVM、构建、隔离模拟器、主设备、真实服务证据。

## 执行

1. 先运行匹配行为的最小模块测试。排盘查黄金/差分、交节/23点/DST/支持边缘；数据查revision、三态、迁移、篡改、预览零写入、提交和中断恢复；UI查状态所有权/返回/布局/导出恢复。
2. 使用Android Studio JBR和仓库wrapper。全模块JVM/静态门禁为 ./gradlew test lintDebug lintRelease；需要实际重跑测试时加 -I scripts/fresh_jvm_tests.init.gradle。从XML汇总失败/错误/跳过与执行时间，不能用Gradle退出0或旧报告文件时间冒充新执行。
3. 构建仅在代码/配置或交付验证确需时执行。文档任务可复核已冻结报告及相关源码未变，不重生成APK；注明旧证据运行时间。需要Release时另做签名/non-debuggable/精确产物核验。
4. UI源码合同不能证明渲染/帧性能。隔离环境使用合成资料，测实际连续点选、滚动、标签、返回、宽窄与导出；声明冷/热、配置生效和宿主负载。
5. 永久禁止所有connected*AndroidTest。主设备禁Debug/测试包、仪器、自动启动、卸载/清数据/数据库操作；覆盖需授权与同签名Release。仓库历史E2E脚本不是解除隔离门禁的许可。
6. 真实OCR/AI/Google/Supabase/恢复需对应输入、配置和真实业务证据，按授权单列；不能从构建或模拟器推出真实服务通过。

## 完成与异常

保存命令、代码范围、运行时间、总数/跳过原因与证据路径；分层写PASS/FAIL/NOT RUN/UNKNOWN，列出边界。门禁失败先保留日志并定位同一红灯，不无目的扩大测试；不清除业务数据作为测试清理。
