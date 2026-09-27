package com.example.taskreminder

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskreminder.alarm.NotificationHelper
import com.example.taskreminder.alarm.OngoingReminderService
import com.example.taskreminder.data.Task
import com.example.taskreminder.ui.HabitDetailScreen
import com.example.taskreminder.ui.PermissionScreen
import com.example.taskreminder.ui.TaskEditScreen
import com.example.taskreminder.ui.TaskListScreen
import com.example.taskreminder.ui.theme.AppTheme
import com.example.taskreminder.util.InteractionCopy
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
    var viewingHabitId by remember { mutableStateOf<Long?>(null) }
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val habitRecords by vm.habitRecords.collectAsStateWithLifecycle()
    val viewingHabit = viewingHabitId?.let { id -> tasks.firstOrNull { it.id == id } }

    // 不管是首次引导还是从菜单重新进入设置，完成后都尝试启动常驻服务。
    val finishOnboarding = {
        prefs.edit().putBoolean("permission_done", true).apply()
        showOnboarding = false
        OngoingReminderService.start(context)
    }

    BackHandler(enabled = editing != null || showOnboarding || viewingHabitId != null) {
        when {
            editing != null -> editing = null
            showOnboarding -> finishOnboarding()
            viewingHabitId != null -> viewingHabitId = null
        }
    }

    when {
        showOnboarding -> PermissionScreen(onDone = finishOnboarding)

        editing != null -> {
            val (_, task) = editing!!
            TaskEditScreen(
                task = task,
                onSave = { updatedTask ->
                    val isNew = task == null || task.id == 0L
                    vm.save(updatedTask)
                    Toast.makeText(context, InteractionCopy.saveSuccess(isNew), Toast.LENGTH_SHORT).show()
                    editing = null
                },
                onCancel = { editing = null }
            )
        }

        viewingHabit != null -> {
            HabitDetailScreen(
                task = viewingHabit,
                records = habitRecords[viewingHabit.id].orEmpty(),
                onBack = { viewingHabitId = null },
                onEdit = { editing = true to viewingHabit },
                onToggle = { vm.toggleComplete(viewingHabit) },
                onSetStatus = { dayStart, status, amount, note ->
                    vm.setHabitStatus(viewingHabit, dayStart, status, amount, note)
                },
                onRecordAmount = { dayStart, amount, note ->
                    vm.recordHabitAmount(viewingHabit, dayStart, amount, note)
                },
                onArchive = { archived -> vm.setHabitArchived(viewingHabit, archived) },
                onDelete = {
                    vm.delete(viewingHabit)
                    viewingHabitId = null
                }
            )
        }

        else -> TaskListScreen(
            vm = vm,
            onEdit = { task -> editing = true to task },
            onNew = { editing = true to null },
            onOpenSettings = { showOnboarding = true },
            onOpenHabit = { task -> viewingHabitId = task.id }
        )
    }
}


