package com.example.taskreminder

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskreminder.alarm.NotificationHelper
import com.example.taskreminder.data.Task
import com.example.taskreminder.ui.PermissionScreen
import com.example.taskreminder.ui.TaskEditScreen
import com.example.taskreminder.ui.TaskListScreen
import com.example.taskreminder.ui.theme.AppTheme
import com.example.taskreminder.util.OngoingNotifier
import com.example.taskreminder.vm.TaskViewModel

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
}

@Composable
private fun AppRoot() {
    val context = LocalContext.current
    val vm: TaskViewModel = viewModel()

    val prefs = remember {
        context.getSharedPreferences("app", Context.MODE_PRIVATE)
    }
    var showPermission by remember {
        mutableStateOf(!prefs.getBoolean("permission_done", false))
    }
    var editing by remember { mutableStateOf<Pair<Boolean, Task?>?>(null) }

    when {
        showPermission -> PermissionScreen(onDone = {
            prefs.edit().putBoolean("permission_done", true).apply()
            showPermission = false
        })
        editing != null -> {
            val (_, task) = editing!!
            TaskEditScreen(
                task = task,
                onSave = { t ->
                    vm.save(t)
                    editing = null
                },
                onCancel = { editing = null }
            )
        }
        else -> TaskListScreen(
            vm = vm,
            onEdit = { task -> editing = true to task },
            onNew = { editing = true to null }
        )
    }
}