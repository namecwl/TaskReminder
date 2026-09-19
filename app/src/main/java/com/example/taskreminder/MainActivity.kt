package com.example.taskreminder

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskreminder.alarm.NotificationHelper
import com.example.taskreminder.alarm.OngoingReminderService
import com.example.taskreminder.data.Task
import com.example.taskreminder.ui.PermissionScreen
import com.example.taskreminder.ui.TaskEditScreen
import com.example.taskreminder.ui.TaskListScreen
import com.example.taskreminder.ui.theme.AppTheme
import com.example.taskreminder.util.OngoingNotifier
import com.example.taskreminder.vm.TaskViewModel

/**
 * 应用入口。
 *
 * 负责初始化通知通道，并在应用进入前台时补启常驻通知服务。
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.ensureChannel(this)
        OngoingNotifier.ensureChannel(this)
        setContent {
            AppTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        OngoingReminderService.start(this)
    }
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val vm: TaskViewModel = viewModel()
    val prefs = remember { context.getSharedPreferences("app", Context.MODE_PRIVATE) }

    var showOnboarding by remember {
        mutableStateOf(!prefs.getBoolean("permission_done", false))
    }
    var editing by remember { mutableStateOf<Pair<Boolean, Task?>?>(null) }

    // 不管是首次引导还是从菜单重新进入设置，完成后都尝试启动常驻服务。
    val finishOnboarding = {
        prefs.edit().putBoolean("permission_done", true).apply()
        showOnboarding = false
        OngoingReminderService.start(context)
    }

    BackHandler(enabled = editing != null || showOnboarding) {
        when {
            editing != null -> editing = null
            showOnboarding -> finishOnboarding()
        }
    }

    when {
        showOnboarding -> PermissionScreen(onDone = finishOnboarding)

        editing != null -> {
            val (_, task) = editing!!
            TaskEditScreen(
                task = task,
                onSave = { updatedTask ->
                    vm.save(updatedTask)
                    editing = null
                },
                onCancel = { editing = null }
            )
        }

        else -> TaskListScreen(
            vm = vm,
            onEdit = { task -> editing = true to task },
            onNew = { editing = true to null },
            onOpenSettings = { showOnboarding = true }
        )
    }
}


