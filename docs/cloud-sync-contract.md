# 南枫云 Google 登录与结构化同步合同

## 范围

南枫八字继续以本机 Room 为业务真值。南枫云只保存经 AES-256-GCM 端到端加密后的独立应用文档，
并通过云端原子版本号提交最新加密备份。

云端结构化快照包含命例、已采用与历史计算快照、断事笔记、事件、分组及标签。它明确不包含：

- 问真来源图片、私有附件与依赖附件的字段证据；
- AI 服务 API Key、调用记录、系统权限或临时导入会话；
- 未经用户主动导出的本机文件。

完整的图片/附件迁移继续使用既有完整备份或命例包流程，不应被云同步入口替代。

## 身份、密钥与恢复

- Google 只提供身份。Credential Manager 生成随机 nonce；Google ID Token 只提交给 Supabase
  校验，不写数据库、日志、诊断包或仓库。
- 南枫云账号密钥封装是共享的；同一 Google 用户在南枫记与南枫八字上使用同一恢复码解锁，
  但两款 App 的云文档和 AES-GCM AAD 均按应用标识隔离。
- 首次创建保险库只展示一次恢复码。用户点击“已安全保存”前，不启动自动同步。
- 本机会话、刷新令牌、保险库密钥和设备同步 ID 仅以 Android Keystore 加密保存。
- 已验证 Google 身份的显示名与头像仅用于账号页。头像经受保护的南枫云代理读取后，按“账号 +
  头像地址”写入应用私有缓存；进页先显示缓存再静默刷新。代理不可用时，只允许向经校验的
  `https://*.googleusercontent.com` 地址直连兜底（不跟随重定向、限制 2 MiB）；缓存不进入命例
  快照、备份或云同步，退出该账号时删除。

## 生命周期

- 空白设备：Google 身份和恢复码均验证后，才可恢复云端结构化快照。
- 非空本机：本机是加密备份源。登录验证完成后，本机命例更新云端；不展示双端冲突页。
- 已建立会话且本机已有命例：本机始终是备份源。与云端不一致时直接上传本机加密快照，不反向恢复、不自动合并、不展示冲突页。
- 空白新设备：Google 身份和恢复码均验证后恢复云端快照；不会用空库覆盖云端。
- 退出账号只删除本机云端会话与后台任务，不删除命例、附件或设置。
- 本机结构化表变更后使用一个有网约束的 30 秒合并 WorkManager 任务；12 小时周期任务兜底。
- 页面离开、App 更新或 WorkManager 停止造成的协程取消是正常控制流，不记为网络失败，不向用户显示 `JobCancellationException` 等内部异常名。

## 本机构建与真实服务前提

local.properties 必须由本机私有配置提供以下公开客户端值，且不得提交：

    nanfeng.cloud.url=
    nanfeng.cloud.publishableKey=
    nanfeng.cloud.googleServerClientId=

在同一 Google Cloud 项目中，必须为 com.nanzhufeng.nanfengbazi 的实际签名登记独立 Android
OAuth 客户端；仅配置 Web Client ID 不足以让 Credential Manager 在八字 App 中完成身份凭据。
Supabase 的 URL、Publishable Key 和 Web Client ID 属于本机构建配置，Client Secret、
service_role、恢复码和真实账号绝不进入八字仓库或测试日志。

## 验收

自动验证至少覆盖：恢复码解锁、AES-GCM 加解密、密文篡改拒绝、云端快照不含附件，以及首次
恢复/本机备份/退出保留本机数据。真实验收还需分别验证 Google 登录、Supabase RLS/RPC、首次
恢复、变更后备份、周期兜底和真实设备数据保留。
