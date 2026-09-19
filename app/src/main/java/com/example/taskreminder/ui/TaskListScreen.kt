package com.example.taskreminder.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import com.example.taskreminder.util.BackupUtil
import com.example.taskreminder.util.NaturalLanguageParser
import com.example.taskreminder.util.OngoingNotifier
import com.example.taskreminder.vm.TaskViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

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
    var showCompleted by remember { mutableStateOf(false) }

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(tasks) {
        OngoingNotifier.update(context, tasks)
    }

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

    val groups = remember(tasks, now) { groupTasks(tasks, now) }
    val nearest = remember(tasks, now) {
        tasks.filter { !it.isCompleted && it.dueTime > now - 60_000L }
            .minByOrNull { it.dueTime }
    }

    var quickInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("任务提醒") },
                actions = {
                    IconButton(onClick = { showCompleted = !showCompleted }) {
                        Icon(
                            if (showCompleted) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = "显示/隐藏已完成"
                        )
                    }
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
            item { NearestBar(nearest, now) }

            item {
                QuickAddBar(
                    value = quickInput,
                    onValueChange = { quickInput = it },
                    onSubmit = {
                        val parsed = NaturalLanguageParser.parse(quickInput)
                        if (parsed.title.isNotBlank()) {
                            vm.save(
                                Task(
                                    title = parsed.title,
                                    note = "",
                                    dueTime = parsed.dueTime,
                                    repeatRule = parsed.repeatRule,
                                    repeatInterval = parsed.repeatInterval,
                                    repeatDaysOfWeek = parsed.repeatDaysOfWeek,
                                    repeatDayOfMonth = parsed.repeatDayOfMonth,
                                    advanceMinutes = 5
                                )
                            )
                            quickInput = ""
                        }
                    }
                )
            }

            item { SectionHeader("今日事件", groups.today.size) }
            if (groups.today.isEmpty()) {
                item { EmptyHint("今天还没有任务") }
            } else {
                items(groups.today, key = { "today_${it.id}" }) { task ->
                    TaskRow(task, now, { onEdit(task) }, { vm.toggleComplete(task) }, { vm.delete(task) })
                }
            }

            item { SectionHeader("习惯", groups.habits.size) }
            if (groups.habits.isEmpty()) {
                item { EmptyHint("试试输入「每天八点吃药」") }
            } else {
                items(groups.habits, key = { "habit_${it.id}" }) { task ->
                    TaskRow(task, now, { onEdit(task) }, { vm.toggleComplete(task) }, { vm.delete(task) })
                }
            }

            item { SectionHeader("未来事件", groups.future.size) }
            if (groups.future.isEmpty()) {
                item { EmptyHint("试试输入「明天下午三点开会」") }
            } else {
                items(groups.future, key = { "future_${it.id}" }) { task ->
                    TaskRow(task, now, { onEdit(task) }, { vm.toggleComplete(task) }, { vm.delete(task) }, showCountdown = true)
                }
            }

            if (showCompleted && groups.completed.isNotEmpty()) {
                item { SectionHeader("已完成", groups.completed.size) }
                items(groups.completed, key = { "done_${it.id}" }) { task ->
                    TaskRow(task, now, { onEdit(task) }, { vm.toggleComplete(task) }, { vm.delete(task) })
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun NearestBar(nearest: Task?, now: Long) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = contentColor)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (nearest == null) {
                    Text("暂无待办", style = MaterialTheme.typography.bodyMedium, color = contentColor)
                } else {
                    Text(
                        "最近：${nearest.title}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = contentColor
                    )
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nearest.dueTime)) +
                            " · " + formatCountdown(nearest.dueTime - now),
                        style = MaterialTheme.typography.bodySmall,
                        color = contentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("快速添加，比如「今天十点二十洗碗」") },
        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = onSubmit) {
                    Icon(Icons.Default.Send, contentDescription = "添加")
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        singleLine = true
    )
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(6.dp))
        if (count > 0) {
            Text(
                "$count",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
private fun TaskRow(
    task: Task,
    now: Long,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    showCountdown: Boolean = false
) {
    val overdue = !task.isCompleted && task.dueTime < now
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
                    formatTime(task, now, showCountdown),
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

private fun formatTime(task: Task, now: Long, showCountdown: Boolean): String {
    val timeStr = if (showCountdown) {
        formatCountdown(task.dueTime - now)
    } else {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.dueTime))
    }
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

private fun formatCountdown(diffMs: Long): String {
    if (diffMs <= 0) return "已过期"
    val totalMin = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val days = totalMin / (60 * 24)
    val hours = (totalMin % (60 * 24)) / 60
    val mins = totalMin % 60
    return when {
        days > 0 -> "还有${days}天${hours}小时"
        hours > 0 -> "还有${hours}小时${mins}分"
        else -> "还有${mins}分"
    }
}

private data class Groups(
    val habits: List<Task>,
    val today: List<Task>,
    val future: List<Task>,
    val completed: List<Task>
)

private fun groupTasks(tasks: List<Task>, now: Long): Groups {
    val todayStart = Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val tomorrowStart = todayStart + 86_400_000L

    val completed = tasks.filter { it.isCompleted }
    val active = tasks.filter { !it.isCompleted }

    val habits = active.filter { it.repeatRule != RepeatRule.NONE }
    val nonRepeat = active.filter { it.repeatRule == RepeatRule.NONE }
    val today = nonRepeat.filter { it.dueTime < tomorrowStart }
    val future = nonRepeat.filter { it.dueTime >= tomorrowStart }.sortedBy { it.dueTime }

    return Groups(habits, today, future, completed)
}