package com.nanzhufeng.nanfengbazi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nanzhufeng.nanfengbazi.cloud.BaziCloudSyncCoordinator
import com.nanzhufeng.nanfengbazi.cloud.BaziCloudSyncState
import com.nanzhufeng.nanfengbazi.cloud.BaziGoogleSignInClient
import com.nanzhufeng.nanfengbazi.cloud.BaziGoogleSignInResult
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@Composable
fun BaziCloudSettingsPage(
    coordinator: BaziCloudSyncCoordinator,
    googleSignInClient: BaziGoogleSignInClient,
    activityContext: Context,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by coordinator.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var recoveryCode by rememberSaveable { mutableStateOf("") }
    var pickerBusy by rememberSaveable { mutableStateOf(false) }
    var pickerError by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmSwitch by rememberSaveable { mutableStateOf(false) }
    var confirmSignOut by rememberSaveable { mutableStateOf(false) }

    fun launchGooglePicker(resetSelection: Boolean) {
        if (pickerBusy) return
        pickerBusy = true
        pickerError = null
        scope.launch {
            try {
                if (resetSelection) googleSignInClient.clearCredentialState()
                when (val result = googleSignInClient.signIn(activityContext)) {
                    is BaziGoogleSignInResult.Success -> coordinator.signInWithGoogle(
                        result.idToken, result.nonce, result.displayName, result.avatarUrl,
                    )
                    BaziGoogleSignInResult.Cancelled -> Unit
                    is BaziGoogleSignInResult.Failure -> pickerError = result.message
                }
            } finally {
                pickerBusy = false
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack, contentPadding = PaddingValues(0.dp)) { Text("‹ 返回设置") }
            Text("南枫云", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Google 账号用于确认身份；云端只保存端到端加密的结构化命例。来源图片、附件、字段证据与 AI 密钥不会上传。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (val current = state) {
            BaziCloudSyncState.Unconfigured -> item {
                CloudPanel("当前构建未配置南枫云") { Text("请使用已配置的正式构建。") }
            }
            BaziCloudSyncState.SignedOut -> item {
                CloudPanel("使用 Google 账号登录") {
                    Text("登录后可查看账号详情、切换账号与同步状态；不会自动上传或覆盖本机命例。")
                    Button(
                        onClick = { launchGooglePicker(resetSelection = false) },
                        enabled = googleSignInClient.configured && !pickerBusy,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (pickerBusy) "正在打开 Google…" else "使用 Google 账号登录") }
                    pickerError?.let { CloudError(it) }
                }
            }
            is BaziCloudSyncState.Working -> item {
                CloudPanel("南枫云正在处理") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(current.message)
                    }
                    Text("本机命例仍可正常使用，请勿重复提交。", style = MaterialTheme.typography.bodySmall)
                }
            }
            is BaziCloudSyncState.RecoveryCodeReady -> item {
                CloudPanel("请保存恢复码") {
                    Text("这是唯一一次展示。Google 与南枫云都无法替你找回。")
                    OutlinedTextField(
                        value = current.code,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("恢复码") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(onClick = {
                        context.getSystemService(ClipboardManager::class.java)
                            ?.setPrimaryClip(ClipData.newPlainText("南枫云恢复码", current.code))
                    }, modifier = Modifier.fillMaxWidth()) { Text("复制恢复码") }
                    Button(onClick = { scope.launch { coordinator.acknowledgeRecoveryCode() } }, modifier = Modifier.fillMaxWidth()) {
                        Text("我已安全保存")
                    }
                }
            }
            BaziCloudSyncState.RecoveryCodeRequired -> item {
                CloudPanel("需要恢复码解锁") {
                    Text("该 Google 账号已有南枫云加密保险库。请输入最初保存的恢复码，验证成功后才会同步。")
                    OutlinedTextField(
                        value = recoveryCode,
                        onValueChange = { recoveryCode = it },
                        label = { Text("恢复码") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = { scope.launch { coordinator.recoverWithCode(recoveryCode.toCharArray()); recoveryCode = "" } },
                        enabled = recoveryCode.filterNot(Char::isWhitespace).length >= 40,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("解锁加密保险库") }
                    TextButton(onClick = coordinator::cancelPendingSignIn, modifier = Modifier.fillMaxWidth()) { Text("取消，保留当前账号") }
                }
            }
            is BaziCloudSyncState.AccountEntryChoice -> item {
                CloudPanel("确认 Google 账号与数据方向") {
                    AccountIdentity(
                        current.displayName,
                        current.email,
                        current.avatarUrl,
                        coordinator::loadCachedGoogleAvatar,
                        coordinator::refreshGoogleAvatar,
                    )
                    Text(
                        when {
                            current.hasRemoteData && current.localHasData -> "本机已有命例。继续后会将本机加密备份更新到云端。"
                            current.hasRemoteData -> "云端已有八字结构化数据。继续后需要恢复码；仅在本机命例库为空时才恢复。"
                            else -> "此账号还没有八字云端数据。继续会为这个账号创建加密保险库并显示一次性恢复码。"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(onClick = { scope.launch { coordinator.continueAccountEntry() } }, modifier = Modifier.fillMaxWidth()) { Text("为此账号继续") }
                    OutlinedButton(onClick = { launchGooglePicker(resetSelection = true) }, enabled = !pickerBusy, modifier = Modifier.fillMaxWidth()) {
                        Text("更换 Google 账号")
                    }
                    TextButton(onClick = coordinator::cancelPendingSignIn, modifier = Modifier.fillMaxWidth()) { Text("取消，保留当前账号") }
                }
            }
            is BaziCloudSyncState.Ready -> item {
                LaunchedEffect(current.email, current.avatarUrl) {
                    if (current.avatarUrl.isNullOrBlank()) coordinator.refreshGoogleIdentity()
                }
                AccountReadyPanel(
                    state = current,
                    pickerBusy = pickerBusy,
                    loadCachedAvatar = coordinator::loadCachedGoogleAvatar,
                    refreshAvatar = coordinator::refreshGoogleAvatar,
                    onManageAccount = { runCatching { uriHandler.openUri("https://myaccount.google.com/") } },
                    onSwitchAccount = { confirmSwitch = true },
                    onSync = { scope.launch { coordinator.syncNow() } },
                    onSignOut = { confirmSignOut = true },
                )
            }
            is BaziCloudSyncState.Failure -> item {
                CloudPanel("同步未完成") {
                    CloudError(current.message)
                    Text("本机数据未被删除或覆盖。", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = coordinator::restorePersistedState, modifier = Modifier.fillMaxWidth()) { Text("返回账号详情") }
                }
            }
        }
    }
    if (confirmSwitch) {
        AlertDialog(
            onDismissRequest = { confirmSwitch = false },
            containerColor = CloudCardWhite,
            title = { Text("切换 Google 账号？") },
            text = { Text("本机命例不会删除。选择其他账号后，将先要求恢复码或确认数据方向，绝不会静默覆盖。") },
            confirmButton = { Button(onClick = { confirmSwitch = false; launchGooglePicker(resetSelection = true) }) { Text("选择其他账号") } },
            dismissButton = { TextButton(onClick = { confirmSwitch = false }) { Text("取消") } },
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            containerColor = CloudCardWhite,
            title = { Text("退出南枫云账号？") },
            text = { Text("只会移除本机云端会话并停止自动同步；本机命例、附件和设置都会保留。") },
            confirmButton = { Button(onClick = { confirmSignOut = false; scope.launch { coordinator.signOut(); googleSignInClient.clearCredentialState() } }) { Text("退出并保留本机数据") } },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun AccountReadyPanel(
    state: BaziCloudSyncState.Ready,
    pickerBusy: Boolean,
    loadCachedAvatar: suspend (String) -> ByteArray?,
    refreshAvatar: suspend (String) -> ByteArray?,
    onManageAccount: () -> Unit,
    onSwitchAccount: () -> Unit,
    onSync: () -> Unit,
    onSignOut: () -> Unit,
) = Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    val title = state.displayName?.takeIf { it.isNotBlank() }
        ?: state.email.substringBefore('@').ifBlank { "Google 用户" }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = CloudCardWhite,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(state.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            GoogleAvatar(title, state.avatarUrl, loadCachedAvatar, refreshAvatar)
            Text("$title，您好！", style = MaterialTheme.typography.titleLarge)
            OutlinedButton(
                onClick = onManageAccount,
                shape = RoundedCornerShape(24.dp),
            ) { Text("管理您的 Google 账号") }
        }
    }
    AccountActionGroup {
        AccountActionRow(
            title = if (pickerBusy) "正在打开账号选择器…" else "切换 Google 账号",
            description = "从此设备上的账号中重新选择",
            icon = { Icon(Icons.Outlined.SwitchAccount, null, tint = CloudActionBlue) },
            iconColor = CloudActionBlue.copy(alpha = 0.12f),
            onClick = onSwitchAccount,
            enabled = !pickerBusy,
        )
        HorizontalDivider(modifier = Modifier.padding(start = 68.dp))
        AccountActionRow(
            title = "退出 Google 账号",
            description = "保留本机命例并停止自动同步",
            icon = { Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = CloudActionRed) },
            iconColor = CloudActionRed.copy(alpha = 0.12f),
            onClick = onSignOut,
        )
    }
    CloudPanel("南枫云同步") {
        Text(
            state.lastSyncedAt?.let { "上次成功同步：${formatCloudTime(it)}" } ?: "尚未建立八字云端同步版本",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(state.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        Text("命例变化后约 30 秒合并同步；系统每 12 小时做一次兜底检查。", style = MaterialTheme.typography.bodySmall)
        Button(onClick = onSync, modifier = Modifier.fillMaxWidth()) { Text("立即同步") }
        Text("上传前会加密；来源图片、附件、字段证据与 AI 密钥不上传。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun AccountActionGroup(content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = CloudCardWhite, tonalElevation = 0.dp) {
        Column(content = content)
    }
}

@Composable
private fun AccountActionRow(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    iconColor: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(onClick = onClick, enabled = enabled, color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(modifier = Modifier.size(42.dp), shape = MaterialTheme.shapes.large, color = iconColor) {
                androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) { icon() }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccountIdentity(
    displayName: String?,
    email: String,
    avatarUrl: String?,
    loadCachedAvatar: suspend (String) -> ByteArray?,
    refreshAvatar: suspend (String) -> ByteArray?,
) {
    val title = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@').ifBlank { "Google 用户" }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        GoogleAvatar(title, avatarUrl, loadCachedAvatar, refreshAvatar)
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(email.ifBlank { "Google 账号已验证" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun GoogleAvatar(
    title: String,
    avatarUrl: String?,
    loadCachedAvatar: suspend (String) -> ByteArray?,
    refreshAvatar: suspend (String) -> ByteArray?,
) {
    var bitmap by remember(avatarUrl) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    LaunchedEffect(avatarUrl) {
        val url = avatarUrl ?: return@LaunchedEffect
        var resolved: androidx.compose.ui.graphics.ImageBitmap? = null
        loadCachedAvatar(url)?.let { bytes ->
            try { resolved = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() }
            finally { bytes.fill(0) }
        }
        refreshAvatar(url)?.let { bytes ->
            try { resolved = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap() ?: resolved }
            finally { bytes.fill(0) }
        }
        bitmap = resolved
    }
    Surface(
        modifier = Modifier.size(82.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        if (bitmap != null) {
            Image(bitmap = checkNotNull(bitmap), contentDescription = "Google 账号头像", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
        } else {
            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                Text(title.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CloudPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = CloudCardWhite, tonalElevation = 0.dp) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun CloudError(message: String) = Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)

private val CloudCardWhite = Color.White
private val CloudActionBlue = Color(0xFF4F93D1)
private val CloudActionRed = Color(0xFFD26878)

private fun formatCloudTime(timestamp: Long): String = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))
