package com.example.taskreminder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class TaskTimeFilter(val label: String) { ALL("全部"), TODAY("今天"), WEEK("本周"), MONTH("本月"), EARLIER("更早") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CompletedTasksScreen(tasks: List<Task>, onBack: () -> Unit, onRestore: (Task) -> Unit, onEdit: (Task) -> Unit, onDelete: (Task) -> Unit) {
    BackHandler(onBack = onBack)
    var timeFilter by remember { mutableStateOf(TaskTimeFilter.ALL) }
    var repeatOnly by remember { mutableStateOf<Boolean?>(null) }
    val completed = remember(tasks, timeFilter, repeatOnly) {
        tasks.filter { task -> task.isCompleted && !task.archived && matchesTime(task.completedAt ?: task.createdAt, timeFilter) && (repeatOnly == null || (task.repeatRule != RepeatRule.NONE) == repeatOnly) }.sortedByDescending { it.completedAt ?: it.createdAt }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("已完成") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { TaskFilterHeader("按时间筛选", TaskTimeFilter.entries.map { it.label to (timeFilter == it) }) { timeFilter = TaskTimeFilter.entries[it] } }
            item { TaskFilterHeader("按类型筛选", listOf("全部" to (repeatOnly == null), "单次任务" to (repeatOnly == false), "重复任务" to (repeatOnly == true))) { repeatOnly = when (it) { 0 -> null; 1 -> false; else -> true } } }
            if (completed.isEmpty()) item { EmptyCollection("没有符合条件的已完成内容") }
            else items(completed, key = { it.id }) { task ->
                CollectionTaskCard(task, "完成于 " + formatDateTime(task.completedAt ?: task.createdAt), if (task.repeatRule == RepeatRule.NONE) "单次任务" else "重复任务") {
                    TextButton(onClick = { onRestore(task) }) { Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("恢复") }
                    TextButton(onClick = { onEdit(task) }) { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("编辑") }
                    TextButton(onClick = { onDelete(task) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchivedItemsScreen(tasks: List<Task>, onBack: () -> Unit, onRestoreTask: (Task) -> Unit, onRestoreHabit: (Task) -> Unit, onOpenHabit: (Task) -> Unit, onEditTask: (Task) -> Unit, onDelete: (Task) -> Unit) {
    BackHandler(onBack = onBack)
    var typeFilter by remember { mutableStateOf<String?>(null) }
    val archivedTasks = tasks.filter { it.archived && it.repeatRule == RepeatRule.NONE }
    val archivedHabits = tasks.filter { it.habitArchived && it.repeatRule != RepeatRule.NONE }
    val total = archivedTasks.size + archivedHabits.size
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("已归档") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item { TaskFilterHeader("按类型筛选", listOf("全部 " + total to (typeFilter == null), "任务 " + archivedTasks.size to (typeFilter == "task"), "习惯 " + archivedHabits.size to (typeFilter == "habit"))) { typeFilter = when (it) { 0 -> null; 1 -> "task"; else -> "habit" } } }
            if (total == 0) item { EmptyCollection("暂无已归档内容") }
            if ((typeFilter == null || typeFilter == "task") && archivedTasks.isNotEmpty()) {
                item { Text("已归档任务", style = MaterialTheme.typography.titleMedium) }
                items(archivedTasks, key = { "task_" + it.id }) { task ->
                    CollectionTaskCard(task, "归档任务", "任务") {
                        TextButton(onClick = { onRestoreTask(task) }) { Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("恢复") }
                        TextButton(onClick = { onEditTask(task) }) { Icon(Icons.Rounded.Edit, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("编辑") }
                        TextButton(onClick = { onDelete(task) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
            if ((typeFilter == null || typeFilter == "habit") && archivedHabits.isNotEmpty()) {
                item { Text("已归档习惯", style = MaterialTheme.typography.titleMedium) }
                items(archivedHabits, key = { "habit_" + it.id }) { task ->
                    CollectionTaskCard(task, "归档习惯 · 累计 " + task.totalCompletions + " 天", "习惯") {
                        TextButton(onClick = { onRestoreHabit(task) }) { Icon(Icons.Rounded.Restore, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("恢复") }
                        TextButton(onClick = { onOpenHabit(task) }) { Icon(Icons.Rounded.Archive, null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("查看") }
                        TextButton(onClick = { onDelete(task) }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskFilterHeader(label: String, options: List<Pair<String, Boolean>>, onSelect: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { index, pair -> Surface(Modifier.clickable { onSelect(index) }, shape = RoundedCornerShape(99.dp), color = if (pair.second) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), border = if (pair.second) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)) else null) { Text(pair.first, Modifier.padding(horizontal = 11.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium) } }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun CollectionTaskCard(task: Task, subtitle: String, badge: String, actions: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))) {
        Column(Modifier.padding(15.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(Modifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) { Box(contentAlignment = Alignment.Center) { Icon(if (badge == "习惯") Icons.Rounded.Archive else Icons.Rounded.CheckCircle, null, modifier = Modifier.size(20.dp)) } }
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) { Text(task.title.ifBlank { "未命名任务" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(badge + " · " + subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (task.note.isNotBlank()) { Spacer(Modifier.height(8.dp)); Text(task.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Spacer(Modifier.height(8.dp)); Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) { actions() }
        }
    }
}

@Composable
private fun EmptyCollection(text: String) { Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) { Text(text, Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) } }

private fun matchesTime(time: Long, filter: TaskTimeFilter): Boolean {
    if (filter == TaskTimeFilter.ALL) return true
    val now = Calendar.getInstance(); val target = Calendar.getInstance().apply { timeInMillis = time }
    return when (filter) {
        TaskTimeFilter.TODAY -> startOfTaskDay(time) == startOfTaskDay(System.currentTimeMillis())
        TaskTimeFilter.WEEK -> { val start = (now.clone() as Calendar).apply { firstDayOfWeek = Calendar.MONDAY; set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }; time >= start.timeInMillis }
        TaskTimeFilter.MONTH -> target.get(Calendar.YEAR) == now.get(Calendar.YEAR) && target.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        TaskTimeFilter.EARLIER -> time < startOfTaskMonth(System.currentTimeMillis())
        TaskTimeFilter.ALL -> true
    }
}

private fun formatDateTime(time: Long) = SimpleDateFormat("M月d日 HH:mm", Locale.SIMPLIFIED_CHINESE).format(Date(time))
private fun startOfTaskMonth(time: Long) = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
private fun startOfTaskDay(time: Long) = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis