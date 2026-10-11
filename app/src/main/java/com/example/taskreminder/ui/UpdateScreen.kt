package com.example.taskreminder.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskreminder.BuildConfig
import com.example.taskreminder.util.UpdateCheckResult
import com.example.taskreminder.util.UpdateInfo
import com.example.taskreminder.util.UpdateManager
import kotlinx.coroutines.launch

private sealed interface UpdateUiState {
    data object Checking : UpdateUiState
    data object UpToDate : UpdateUiState
    data class Available(val info: UpdateInfo) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateUiState>(UpdateUiState.Checking) }
    var downloading by remember { mutableStateOf(false) }
    var installMessage by remember { mutableStateOf<String?>(null) }

    suspend fun checkUpdate() {
        state = UpdateUiState.Checking
        installMessage = null
        state = when (val result = UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)) {
            UpdateCheckResult.UpToDate -> UpdateUiState.UpToDate
            is UpdateCheckResult.Available -> UpdateUiState.Available(result.info)
            is UpdateCheckResult.Failed -> UpdateUiState.Failed(result.message)
        }
    }

    LaunchedEffect(Unit) { checkUpdate() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text("软件更新", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "当前版本 ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            when (val current = state) {
                UpdateUiState.Checking -> UpdateStatusCard(
                    icon = {
                        CircularProgressIndicator(modifier = Modifier.size(38.dp), strokeWidth = 3.dp)
                    },
                    title = "正在检查更新",
                    subtitle = "正在连接 GitHub Releases"
                )
                UpdateUiState.UpToDate -> UpdateStatusCard(
                    icon = {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(42.dp)
                        )
                    },
                    title = "已经是最新版本",
                    subtitle = "当前版本 ${BuildConfig.VERSION_NAME}"
                )
                is UpdateUiState.Available -> {
                    UpdateStatusCard(
                        icon = {
                            Icon(
                                Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            )
                        },
                        title = "发现新版本 ${current.info.version}",
                        subtitle = "可覆盖安装，本地打卡数据会保留"
                    )
                    Spacer(Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text("更新说明", style = MaterialTheme.typography.titleSmall)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                current.info.notes.ifBlank { "本次更新包含体验与稳定性优化。" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 12,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            if (!downloading) {
                                downloading = true
                                installMessage = null
                                scope.launch {
                                    UpdateManager.downloadApk(context, current.info)
                                        .onSuccess { apk ->
                                            UpdateManager.installApk(context, apk)
                                                .onSuccess { openedInstaller ->
                                                    installMessage = if (openedInstaller) {
                                                        "已打开系统安装器。覆盖安装不会清除本地数据。"
                                                    } else {
                                                        "请先允许本应用安装未知来源应用，然后返回再次点击安装。"
                                                    }
                                                }
                                                .onFailure { installMessage = "无法打开安装器：${it.message}" }
                                        }
                                        .onFailure { installMessage = "下载失败：${it.message}" }
                                    downloading = false
                                }
                            }
                        },
                        enabled = !downloading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        if (downloading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(9.dp))
                            Text("正在下载…")
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("下载并覆盖安装")
                        }
                    }
                    if (!current.info.releaseUrl.isBlank()) {
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(current.info.releaseUrl))
                                )
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Icon(
                                Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text("查看 GitHub Release")
                        }
                    }
                }
                is UpdateUiState.Failed -> {
                    UpdateStatusCard(
                        icon = {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(42.dp)
                            )
                        },
                        title = "检查更新失败",
                        subtitle = current.message
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { scope.launch { checkUpdate() } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("重新检查")
                    }
                }
            }

            installMessage?.let { message ->
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Text(
                        message,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
                    Text("关于覆盖安装", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "系统会用新 APK 覆盖旧版本，不会卸载应用，也不会主动清除打卡数据。请确保新旧 APK 使用同一个签名证书。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateStatusCard(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                icon()
            }
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


@Composable
fun UpdateAvailableDialog(
    info: UpdateInfo,
    onLater: () -> Unit,
    onViewUpdate: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = { Icon(Icons.Default.NewReleases, contentDescription = null) },
        title = { Text("发现新版本 ${info.version}") },
        text = {
            Text(
                info.notes.ifBlank { "本次更新包含体验与稳定性优化，可前往更新页面手动下载安装。" },
                maxLines = 8,
                overflow = TextOverflow.Ellipsis
            )
        },
        confirmButton = {
            Button(onClick = onViewUpdate) { Text("查看更新") }
        },
        dismissButton = {
            TextButton(onClick = onLater) { Text("稍后") }
        }
    )
}
