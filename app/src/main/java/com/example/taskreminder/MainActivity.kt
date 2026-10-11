package com.example.taskreminder

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.taskreminder.BuildConfig
import com.example.taskreminder.ui.ArchivedItemsScreen
import com.example.taskreminder.ui.CompletedTasksScreen
import com.example.taskreminder.ui.HabitDetailScreen
import com.example.taskreminder.ui.PermissionScreen
import com.example.taskreminder.ui.TaskEditScreen
import com.example.taskreminder.ui.TaskListScreen
import com.example.taskreminder.ui.UpdateAvailableDialog
import com.example.taskreminder.ui.UpdateScreen
import com.example.taskreminder.ui.theme.AppTheme
import com.example.taskreminder.util.InteractionCopy
import com.example.taskreminder.util.OngoingNotifier
import com.example.taskreminder.util.UpdateCheckResult
import com.example.taskreminder.util.UpdateInfo
import com.example.taskreminder.util.UpdateManager
import com.example.taskreminder.vm.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    var showingUpdate by remember { mutableStateOf(false) }
    var showingCompleted by remember { mutableStateOf(false) }
    var showingArchive by remember { mutableStateOf(false) }
    var pendingUpdate by remember { mutableStateOf<UpdateInfo?>(null) }
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val habitRecords by vm.habitRecords.collectAsStateWithLifecycle()
    val viewingHabit = viewingHabitId?.let { id -> tasks.firstOrNull { it.id == id } }

    LaunchedEffect(Unit) {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        if (prefs.getString("update_check_day", null) != day) {
            when (val result = UpdateManager.checkForUpdate(BuildConfig.VERSION_NAME)) {
                is UpdateCheckResult.Available -> {
                    prefs.edit().putString("update_check_day", day).apply()
                    pendingUpdate = result.info
                }
                UpdateCheckResult.UpToDate -> prefs.edit().putString("update_check_day", day).apply()
                is UpdateCheckResult.Failed -> Unit
            }
        }
    }

    // 不管是首次引导还是从菜单重新进入设置，完成后都尝试启动常驻服务。
    val finishOnboarding = {
        prefs.edit().putBoolean("permission_done", true).apply()
        showOnboarding = false
        OngoingReminderService.start(context)
    }

    BackHandler(enabled = editing != null || showOnboarding || viewingHabitId != null || showingUpdate || showingCompleted || showingArchive) {
        when {
            editing != null -> editing = null
            showOnboarding -> finishOnboarding()
            viewingHabitId != null -> viewingHabitId = null
            showingUpdate -> showingUpdate = false
            showingCompleted -> showingCompleted = false
            showingArchive -> showingArchive = false
        }
    }

    pendingUpdate?.let { info ->
        UpdateAvailableDialog(
            info = info,
            onLater = { pendingUpdate = null },
            onViewUpdate = {
                pendingUpdate = null
                showingUpdate = true
            }
        )
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

        showingUpdate -> UpdateScreen(onBack = { showingUpdate = false })

        showingCompleted -> CompletedTasksScreen(
            tasks = tasks,
            onBack = { showingCompleted = false },
            onRestore = { task -> vm.toggleComplete(task) },
            onEdit = { task -> editing = true to task },
            onDelete = { task -> vm.delete(task) }
        )

        showingArchive -> ArchivedItemsScreen(
            tasks = tasks,
            onBack = { showingArchive = false },
            onRestoreTask = { task -> vm.setTaskArchived(task, false) },
            onRestoreHabit = { task -> vm.setHabitArchived(task, false) },
            onOpenHabit = { task -> viewingHabitId = task.id },
            onEditTask = { task -> editing = true to task },
            onDelete = { task -> vm.delete(task) }
        )

        else -> TaskListScreen(
            vm = vm,
            onEdit = { task -> editing = true to task },
            onNew = { editing = true to null },
            onOpenSettings = { showOnboarding = true },
            onOpenHabit = { task -> viewingHabitId = task.id },
            onOpenUpdate = { showingUpdate = true },
            onOpenCompleted = { showingCompleted = true },
            onOpenArchive = { showingArchive = true }
        )
    }
}


