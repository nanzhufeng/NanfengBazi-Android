# 南枫八字 Android 长期开发契约

## 事实源与职责

- 当前用户指令优先。进入项目先核对 checkout、工作区和相关需求/合同；实现与适用验证优先于旧文档，冲突记录于开发档案，不凭空补全。
- 合同读取顺序与唯一正文见docs/CONTRACT_INDEX.md；本文件只保留长期约束；架构、领域、数据、云分别以 docs/architecture-governance.md、domain-rules.md、DATA_CONTRACT.md、cloud-sync-contract.md 为正文。当前状态放 PROJECT_STATUS.md，任务接续放 CURRENT_HANDOFF.md，决策放 decision-log.md。
- 开发、排错、测试、审查、文档同步按 .agents/skills/ 对应独立流程执行；状态文件按个人 maintain-codex-workflow 的 project-status-sop 维护。

## 唯一所有者与数据安全

- 排盘只经 BaziEngine.calculate()；专业岁运只经 ProfessionalFortuneResolver。页面不复制算法。算法/时间口径变更须扩展黄金、边界及选择回定位测试。
- 命例增量写入经 CaseRepository；完整备份/恢复和云快照由 CaseBackupService 拥有，单命例交换由既有交换服务拥有。特权批量事务留在数据层，UI不得直写DAO；禁止 destructive migration。
- Room是本机业务真值；详情、笔记、事件绑定同一 caseId + revision。缓存未命中不等于业务数据为空；来源值/计算值/采用值与文本ABSENT/PRESENT/CLEARED不能合并。
- 导入/恢复先校验路径、容量、哈希、数量、引用及冲突，再按显式计划提交；预览零写入，提交重新验证文件和修订号，失败/中断保留恢复边界。
- 不提交真实命例、附件、OCR原文、账号、令牌、恢复码、云密文、私有配置或密钥。凭据以Keystore密钥加密保存，取消作为正常控制流传播。
- AI调用由用户主动触发并显示发送范围；云先经用户启用/身份和恢复码确认，再按云合同允许后台备份。外部结果不得覆盖本地计算真值；云快照排除来源图片、附件、字段证据与模型密钥。

## 状态、交互与性能

- UI重排复用现有用例与状态链路，不复制ViewModel/仓储/备份/同步。派生关系与分析来自可测试领域事实，不以固定文案伪装个案结论。
- 目录恢复、维护、校准与慢写盘分离；投影可丢弃，异步持久化须守卫代次。正确首帧来自同案例完整快照，过期任务不得覆盖新选择。
- 标签切换不得隐式重置专业观察时刻；视口/轴选择按案例持有，宽窄布局分别处理。导出复用真实渲染器并在成功/失败/取消后恢复现场。
- 表面、clip、点击、涟漪、按压和焦点共用形状；缩小视觉仍保留可用命中区域，滚动尾部避让悬浮导航。新增入口补相应行为/交互合同。
- 点击反馈与内容完成分开，不插固定等待。性能报告区分帧、启动、点击响应及工具空闲时间；配置文件存在不证明平台优化已生效。

## 验证与交付

- 按风险选最小充分验证，分开报告源码、JVM、静态、构建、模拟器、主设备和真实服务。构建/安装通过不等于业务或性能验收。
- 永久禁止所有 connected*AndroidTest。主设备禁止Debug/测试包、仪器测试、自动启动、卸载、清数据或直接操作数据库。
- 授权覆盖只核对目标设备、包名、版本、non-debuggable、同证书、APK哈希与数据指纹后，以同签名Release主APK执行 pm install -r --user 0。沿用现装匹配的原签名正常更新，不因新签名规则强迫旧项目换签/迁移。
- 凭据接入、原身份例外与正式交付按 develop-apps-with-nanfeng-product-standards/references/delivery-and-acceptance.md；不得输出凭据或借卸载绕过身份不匹配。
- 提交前核查相关门禁与 git diff --check，保留无关工作区变化。已冻结且源码未变的交付物不为文档/checkpoint重新生成；历史证据复核不刷新原运行时间。
