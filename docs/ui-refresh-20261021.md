# 南枫记设计语言适配 · 2026-10-21

基线：66f95c3，WORKTREE。用户要求重整界面与图标，随后明确保留启动图标；底部导航白色；以用户提供的黑色实心导航/橙色选中参考为最终依据。

## 实现范围

- 六皮肤继续共用页面结构，轻调绿/暖/靛页面底，白卡改用明度差与既有短阴影分层，去除共享装饰描边。
- 主导航白色；排盘/记录/设置分别为成熟图标库的实心首页/书册/齿轮；未选深黑、选中橙色加粗，保留标签与选中语义，无大块选中底。展开侧栏同步色彩与图标。
- 返回统一共享折角图标，遗漏的编辑/导入/比较等标准顶栏居中；设置、AI本地子页自绘标题居中；顶部返回/关闭/刷新图标化。
- 设置功能入口分别使用同步、AI、文件备份、恢复、图片检索、网页、人物、文件导入与时钟图标。原功能及回调不变。
- 共享选择控件使用胶囊与浅表面，选中有勾选/字重及语义信号，保留48dp最小高度。

没有新增功能或常驻入口，没有修改领域/仓储/调度逻辑、启动图标、签名或版本配置。旧流畅度与状态机制继续沿用。

## 验证与边界

实际命令：`:app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline`（Android Studio JBR）。最终PASS：App 327项、0失败/错误、1项既有外部样本跳过；Debug构建成功；Lint 0错误/43警告/2信息。最终白色实心图标版设置截图已核对（final-settings.png），主页/记录同一共享导航实现。

专用NanfengBaziCompatibilityAcceptance_API35模拟器 emulator-5600，1140×2616、442dpi、字体1.0；仅使用既有合成命例。主导航/首页/记录/设置截图核对，初次更新安装后一次层级读取暂不可用，重试成功；不把该脚本失败归因于产品状态。

Debug采样与构建负载下存在慢帧，不作性能提升数字或零延迟声明。全部六皮肤×页面、大字体/宽屏和主设备视觉未完整验收；没有触碰主设备、安装测试包或执行connected测试。未正式签名发布/覆盖安装。

本地构建日志：/tmp/bazi-ui-final-20261021.log；模拟器截图与采样：/tmp/bazi-ui-20261021/。临时证据不承诺长期存续。早期深色导航/线框图标截图已被用户否定，不作为最终设计依据。

## 设置文案追加简化

用户要求标题与小字更简洁准确。合并外观、账号与AI分组；备份/恢复、文件导入与内置案例同步以实际动作为名。删除AI服务和关于入口重复介绍；保留备份范围、确认后恢复、文件来源及当前换日规则。截图入口实际注入AI视觉引擎，纠正“本机识别”的错误表述。启动图标、功能回调与数据行为未改。

本轮定向门禁日志：/tmp/bazi-settings-copy-20261021.log；界面文案更新后未重新执行完整视觉矩阵。

设置文案定向验证：InteractionShapeContractTest 58项、0失败/错误，assembleDebug通过；git diff --check通过。未重跑与纯文案无关的领域回归。

首页时间动效追加：BaziHomeSolarClock通过graphicsLayer读取头图既有pulseProgress/flowProgress，点击轻微上浮/缩放、横拖反向位移；秒钟更新仍在独立组件中，无新增循环动画或计时器。

万年历背景追加：按用户截图移除非选中日期格浅灰底与八字排盘内部底板，改为透明以露出白卡；当前选中日期、今日标记与时辰选择行为保持。


## 2026-10-22 星座徽标与目录缺失字段

- 默认星座徽标由46dp缩为36.8dp，符号与文字沿原比例同步缩20%；记录行点击区域不变，已有明确小尺寸的合盘徽标保持原尺寸。
- 已确认代码缺陷：StageTwoViewModel 的 BaziCase.toCaseSummary 在保存/编辑后的即时摘要中遗漏 zodiac/westernZodiac，而 RoomCaseRepository.toSummary 会携带这些字段。现补齐字段并将发布后的目录同步至页面副本。
- 旧磁盘投影、仓储读取和增量目录更新统一在IO调度器补齐缺少的星座；复用Tyme历法星座，采用结果优先，再用规范公历或出生公历/农历。仅派生目录展示，不改Room真值，不在行重组/滑动时排盘。无效日期保持空值并显示“未识别”，不伪装后台任务。
- 新增CaseCatalogZodiacTest覆盖即时摘要保留字段、旧缓存恢复、增量更新、已有采用值优先、农历转换、规范公历优先及无效日期。
- 最终验证：CaseCatalogZodiacTest/CaseCatalogStoreTest/StageTwoViewModelTest/InteractionShapeContractTest共207项，0失败/0错误/0跳过；assembleDebug通过；合同检查18文件/50链接通过；定向diff --check通过。日志：/tmp/bazi-zodiac-final-20261022.log。
- 未读取截图中命例的真实数据库；代码链路缺陷已确认，不能断言该条记录唯一成因。未运行真机/模拟器视觉验收，未覆盖安装或发布。

## 2026-10-22 当前界面增量覆盖安装

- assembleRelease + lintRelease PASS，复用此前同源码定向207项测试证据。
- OPPO PKH120（3B157F009E800000）：原签名、非debuggable Release 1.0.16(10048)，pm install -r --user 0 返回Success；未启动或自动化操作App。
- 安装前后appId、CE/DE数据目录inode、目录路径和首次安装时间一致；安装后拉回APK SHA256与冻结候选一致。未逐条验证业务数据或真实交互。
- 原证书SHA256：0f89bc92cb127895e6881cda9d3c3c641e0efc0f39a9728eedf2585f8e12fdf3。沿用项目既有身份。
- 冻结APK：/tmp/bazi-overlay-20261022/NanfengBazi-Android-v1.0.16-release.apk；SHA256：b9b37414be434eabe595998862bec72c6cb75019eec4995920de567c9f4ef60f。
- 构建、安装、前后元数据及验签记录：/tmp/bazi-overlay-20261022/。当前工作区未提交，本轮未上传GitHub。


## 2026-10-22 功能图标统一与南枫云精简

### 实现

- 新增BaziFeatureIcons.kt，集中管理圆润实心功能图形及少量语义配色；首页合盘/万年历、设置项、AI服务二级入口、合盘记录入口和列表、基础排盘/合盘AI指令统一无装饰底块。备份使用本地保存图形，网页使用蓝色，换日使用紫色，AI使用橙色；底栏沿用白底/深色主体/橙色选中。
- 清除合盘添加图标的浅色圆底，保留原命中范围、角色标签及必要状态；头像、星座徽标、选中态继续表达各自语义。未改启动图标。
- 南枫云标题居中、图标返回；账号切换/退出图标去底块，删重复小字，保留恢复码、数据方向、退出后本机保留及同步范围。删除登录页“不会自动上传”的过时说明：已确认账号及恢复码后实际会自动备份。

### 同步排查与修复边界

- 用户报告最近同步停在11号。代码确认ALREADY_CURRENT（成功读取云端元数据，版本与本机指纹一致）只返回Ready，不更新持久化时间；因此旧上传时间不能证明近期未检查，也不能反证实际漏传。
- 新增可选lastCheckedAt，旧会话可兼容读取；仅在云端确认一致、或上传回读/恢复成功后更新。页面优先显示“最近检查”，旧会话无该值时显示“最近备份”；原lastSyncedAt保留上传/恢复语义。登录恢复、打开页面、本机无变化跳过和失败均不刷新检查时间。
- 本机变更预检查的capture异常原先可绕过同步异常分类。现在进入既有失败/退避分类，CancellationException原样传播，防止可重试I/O失败直接终结该次Worker。
- 主设备只读调度元数据显示BaziCloudSyncWorker已注册，ACTIVE，网络约束满足，等待下次周期时限。摘要：/tmp/bazi-cloud-job-summary-20261022.txt。它只能证明调度存在，不能证明历史上传成功。后续设备断开，未取得账号失败码/真实同步结果，未访问用户云端或触发真实上传。

### 验证和候选包

- 当前App单测331项，0失败、0错误、1项既有跳过；新增时间语义回归，云策略8项通过。Debug/Release构建及两变体Lint通过（各0错误、44警告、2信息）。日志：/tmp/bazi-feature-icons-cloud-final-20261022.log、/tmp/bazi-feature-icons-cloud-release-20261022.log。
- 隔离API35模拟器，合成命例，1140×2616：核对首页、合盘入口、万年历、设置上下部、未登录南枫云、AI服务、记录。截图及联系表：/tmp/bazi-ui-final-20261022/；系统UI冷启动ANR/短暂无节点的初次采样无效，等待系统恢复后重新取证。
- 当前候选为原签名、非debuggable Release 1.0.16(10048)：/tmp/bazi-ui-cloud-release-20261022/NanfengBazi-Android-v1.0.16-release.apk。
- SHA256：57e553a0de11570d68fdf3ce6ccb0051a29f96dccf75915466f75310259a4f57。证书SHA256与先前现装一致（0f89bc92cb127895e6881cda9d3c3c641e0efc0f39a9728eedf2585f8e12fdf3）。本候选尚未覆盖手机、未上传GitHub。
- 未验证：真实账号本轮上传/恢复、已登录云页实际画面、完整六皮肤/宽屏/大字体/所有弹窗矩阵。不把本次显示修复表述成真实账号漏传已恢复。

## 2026-10-22 本轮候选已覆盖OPPO

- 用户授权覆盖后，57e553a0de11570d68fdf3ce6ccb0051a29f96dccf75915466f75310259a4f57候选已同签名Release保数据覆盖PKH120；pm install -r --user 0返回Success。
- 覆盖前后appId、CE/DE数据目录inode、数据目录路径与首次安装时间一致；安装后APK拉回SHA256与候选一致。未卸载/清数据，未启动或自动化操作App；真实账号同步和业务交互仍待验证。
- 证据：/tmp/bazi-ui-cloud-overlay-20261022/result.json及该目录安装前后元数据/日志。以下“未覆盖”描述为覆盖前历史状态。


## 2026-10-22 模型设置与关于页白色承载面

- 按用户两张截图定位：共享AiCommentarySettingsEditor（同一表单的页面/弹窗入口）原API Key输入框默认透明；模型选择框shadowElevation=1dp、关于卡片shadowElevation=2dp导致边缘阴影。
- 模型选择框改纯白、零tonal/shadow elevation、无border；API Key输入框的focused/unfocused/disabled/error容器均白色，普通/禁用边框透明，保留焦点和错误指示；服务商切换承托面纯白，选中信号保留。
- 取消、保存改白色胶囊，保留点击回调、保存标签、触控高度；保存使用强调色文字与字重表达主操作。关于卡片纯白、无阴影/外边框，内部内容/分隔/链接不变。
- 定向InteractionShapeContractTest 58项，0失败/0错误/0跳过，assembleDebug通过；日志/tmp/bazi-white-fields-about-20261022.log。未改AI配置存储、调用或云同步逻辑。本轮未生成新Release/覆盖手机，手机现装仍为57e553a0…上一轮包。

- 视觉核对：隔离API35模拟器1140×2616，玄墨与霜白桃花皮肤均核对两页；最终截图及联系表在/tmp/bazi-white-fields-qa-20261022/，确认模型/密钥/操作按钮白底、关于卡片无外框阴影。本轮未保存密钥或调用服务。

## 2026-10-22 模型设置/关于白底版已覆盖

- assembleRelease + lintRelease通过，沿用58项定向测试与两皮肤模拟器核对证据。
- OPPO PKH120同签名非debuggable Release 1.0.16(10048)以pm install -r --user 0覆盖Success；appId、CE/DE目录inode、路径与首次安装时间不变，APK回读哈希一致。未启动/自动化操作App、未卸载/清数据。
- 当前现装SHA256：56997e7e7a199d35bfcb57f91c466a04c7fe67d8974d8ba3ec5abf482186c22e；候选及安装证据：/tmp/bazi-white-fields-overlay-20261022/；构建日志/tmp/bazi-white-fields-release-20261022.log。
- 以下未覆盖说明是覆盖前历史状态；本轮没有实际云同步验收或GitHub发布。


## 2026-10-25 详情身份栏星座补齐

- 根因：上轮只补齐目录星座，WenzhenCaseIdentityHeader仍单独读取采用结果basicChartDetails.westernZodiac；旧导入快照缺该字段时显示“待计算”。
- 新增共享CaseZodiacPresentation.kt：优先已有采用星座，否则用既有TymeWesternZodiacResolver按规范公历/原始公历或农历补齐。目录和详情共用规则；详情remember只随出生日期、规范日期或采用值变化更新，不随流年/月/日选择重复计算，不重新排盘或改写命例。
- 无法识别的无效日期显示“未识别”，不伪装有后台计算。基本信息、基本排盘、专业细盘共用修复后的身份栏。
- 回归先红后绿：6项新测试在原逻辑4失败；修复后新旧相关213项0失败/错误/跳过，assembleDebug通过。隔离模拟器已有合成旧样本缺basicChartDetails，三个详情页实际均显示摩羯座与相应图标，无待计算。未在主设备操作数据库或执行测试/自动化。
- 证据：[验收记录](verification/case-identity-zodiac-20261025.json)，日志/tmp/bazi-identity-zodiac-{red,green}-20261025.log，截图/tmp/bazi-identity-qa-20261025/。本轮未Release打包、未覆盖手机；10月22日已安装包不含本修复。

## 2026-10-25 详情星座修复已覆盖OPPO

- 用户授权后，assembleRelease + lintRelease通过；原签名非debuggable Release 1.0.16(10048)已以pm install -r --user 0覆盖PKH120，返回Success。
- 覆盖前后appId、CE/DE目录inode、路径和首次安装时间一致；安装后APK回读SHA256与冻结候选一致：fca73621738d153199e19762ca555aafb40d49203c3ed0c3ec747070cf610184。源码167文件摘要与构建前一致。
- 证据及冻结包：/tmp/bazi-identity-overlay-20261025/；构建日志/tmp/bazi-identity-zodiac-release-20261025.log。未卸载/清数据、未启动或UI自动化操作主设备；业务真实路径仍待用户实际操作。
- 以下未Release打包/未覆盖描述为覆盖前历史状态；未修改桌面图标或发布GitHub。
