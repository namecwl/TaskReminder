package com.example.taskreminder.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import com.example.taskreminder.util.BackupUtil
import com.example.taskreminder.vm.TaskViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    vm: TaskViewModel,
    onEdit: (Task) -> Unit,
    onNew: () -> Unit
) {
    val context = LocalContext.current
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                BackupUtil.writeToUri(context, uri, BackupUtil.toJson(tasks))
                Toast.makeText(context, "已导出 ${tasks.size} 条", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导出失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = BackupUtil.readFromUri(context, uri)
                val list = BackupUtil.fromJson(json)
                vm.importTasks(list)
                Toast.makeText(context, "已导入 ${list.size} 条", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val groups = remember(tasks) { groupTasks(tasks) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务提醒") },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "更多")
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("导出备份") },
                            onClick = {
                                menuOpen = false
                                val name = "task_backup_" +
                                    SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault())
                                        .format(Date()) + ".json"
                                exportLauncher.launch(name)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("导入备份") },
                            onClick = {
                                menuOpen = false
                                importLauncher.launch(arrayOf("application/json", "*/*"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("清理已完成") },
                            onClick = {
                                menuOpen = false
                                vm.clearCompleted()
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = "新建")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEach { (title, list) ->
                if (list.isNotEmpty()) {
                    item(key = "header_$title") {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(list, key = { it.id }) { task ->
                        TaskRow(
                            task = task,
                            onClick = { onEdit(task) },
                            onToggle = { vm.toggleComplete(task) },
                            onDelete = { vm.delete(task) }
                        )
                    }
                }
            }
            if (tasks.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "还没有任务，点右下角 + 添加",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TaskRow(
    task: Task,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val overdue = !task.isCompleted && task.dueTime < System.currentTimeMillis()
    val containerColor = when {
        overdue -> MaterialTheme.colorScheme.errorContainer
        task.isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (overdue) MaterialTheme.colorScheme.onErrorContainer
    else MaterialTheme.colorScheme.onSurface

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle() })
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                )
                Text(
                    formatTime(task),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (overdue) MaterialTheme.colorScheme.onErrorContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.note.isNotBlank()) {
                    Text(
                        task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) MaterialTheme.colorScheme.onErrorContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除")
            }
        }
    }
}

private fun formatTime(task: Task): String {
    val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val timeStr = fmt.format(Date(task.dueTime))
    val repeatStr = when (task.repeatRule) {
        RepeatRule.NONE -> ""
        RepeatRule.DAILY -> " · 每天"
        RepeatRule.WEEKDAY -> " · 工作日"
        RepeatRule.WEEKLY -> " · 每周${weekDaysLabel(task.repeatDaysOfWeek)}"
        RepeatRule.MONTHLY -> " · 每月${task.repeatDayOfMonth}号"
        RepeatRule.INTERVAL -> " · 每隔${task.repeatInterval}天"
        else -> ""
    }
    val advanceStr = if (task.advanceMinutes > 0) " · 提前${task.advanceMinutes}分" else ""
    return timeStr + repeatStr + advanceStr
}

private fun weekDaysLabel(csv: String): String {
    val map = mapOf(
        "1" to "一", "2" to "二", "3" to "三",
        "4" to "四", "5" to "五", "6" to "六", "7" to "日"
    )
    return csv.split(",").filter { it.isNotBlank() }
        .mapNotNull { map[it] }
        .joinToString("、")
}

private fun groupTasks(tasks: List<Task>): List<Pair<String, List<Task>>> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrow = today + 86_400_000L
    val dayAfter = tomorrow + 86_400_000L

    val (done, active) = tasks.partition { it.isCompleted }
    val todayList = active.filter { it.dueTime < tomorrow }
    val tomorrowList = active.filter { it.dueTime in tomorrow until dayAfter }
    val laterList = active.filter { it.dueTime >= dayAfter }

    return listOf(
        "今天" to todayList,
        "明天" to tomorrowList,
        "以后" to laterList,
        "已完成" to done
    )
}