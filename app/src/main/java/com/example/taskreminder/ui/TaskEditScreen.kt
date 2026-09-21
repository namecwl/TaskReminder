package com.example.taskreminder.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 新建和编辑任务页面。
 *
 * 页面按“任务内容、提醒时间、重复、提前提醒”分为独立卡片，
 * 保存时统一校验标题、重复间隔和提前分钟数。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    task: Task?,
    onSave: (Task) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val isNew = task == null || task.id == 0L

    var title by remember { mutableStateOf(task?.title ?: "") }
    var note by remember { mutableStateOf(task?.note ?: "") }
    var dueTime by remember { mutableStateOf(task?.dueTime ?: defaultDueTime()) }
    var repeatRule by remember { mutableStateOf(task?.repeatRule ?: RepeatRule.NONE) }
    var repeatInterval by remember {
        mutableStateOf((task?.repeatInterval ?: 2).toString())
    }
    var repeatDays by remember { mutableStateOf(task?.repeatDaysOfWeek ?: "") }
    var dayOfMonth by remember { mutableStateOf(task?.repeatDayOfMonth ?: 1) }
    var advance by remember {
        mutableStateOf((task?.advanceMinutes ?: 5).toString())
    }
    var streakInput by remember { mutableStateOf((task?.streak ?: 0).toString()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val saveTask: () -> Unit = {
        if (title.isBlank()) {
            Toast.makeText(context, "请先填写任务标题", Toast.LENGTH_SHORT).show()
        } else {
            val newStreak = if (repeatRule == RepeatRule.NONE) {
                0
            } else {
                (streakInput.toIntOrNull() ?: 0).coerceAtLeast(0)
            }
            val newTotal = if (repeatRule == RepeatRule.NONE) {
                0
            } else if (task != null && newStreak == task.streak) {
                task.totalCompletions.coerceAtLeast(newStreak)
            } else {
                newStreak
            }
            val existingLastDay = task?.lastCompletedDay ?: 0L
            val newLastCompletedDay = when {
                repeatRule == RepeatRule.NONE -> 0L
                existingLastDay > 0L -> existingLastDay
                newStreak > 0 -> yesterdayStart()
                else -> 0L
            }
            onSave(
                (task ?: Task()).copy(
                    title = title.trim(),
                    note = note.trim(),
                    dueTime = dueTime,
                    repeatRule = repeatRule,
                    repeatInterval = (repeatInterval.toIntOrNull() ?: 2).coerceAtLeast(1),
                    repeatDaysOfWeek = if (repeatRule == RepeatRule.WEEKLY) repeatDays else "",
                    repeatDayOfMonth = dayOfMonth.coerceIn(1, 31),
                    advanceMinutes = (advance.toIntOrNull() ?: 0).coerceIn(0, 1440),
                    isCompleted = false,
                    completedAt = null,
                    enabled = true,
                    streak = newStreak,
                    totalCompletions = newTotal,
                    lastCompletedDay = newLastCompletedDay
                )
            )
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isNew) "新建任务" else "编辑任务",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Rounded.Close, contentDescription = "取消")
                    }
                },
                actions = {
                    TextButton(onClick = saveTask) {
                        Text("保存", fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = saveTask,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (isNew) "创建任务" else "保存修改", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EditSectionCard(
                icon = Icons.Rounded.EditNote,
                title = "任务内容",
                subtitle = "标题建议简短、明确"
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    placeholder = { Text("例如：给客户回电话") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    placeholder = { Text("补充地点、材料或注意事项") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            EditSectionCard(
                icon = Icons.Rounded.AccessTime,
                title = "提醒时间",
                subtitle = "到点后按通知设置提醒"
            ) {
                val quickDates = listOf(
                    "今天" to 0,
                    "明天" to 1,
                    "后天" to 2,
                    "一周后" to 7
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    quickDates.forEach { (label, offset) ->
                        val target = shiftDay(dueTime, offset)
                        FilterChip(
                            selected = isSameDay(dueTime, target),
                            onClick = { dueTime = shiftDay(dueTime, offset) },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PickerTile(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "日期",
                        value = SimpleDateFormat("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
                            .format(Date(dueTime)),
                        modifier = Modifier.weight(1f),
                        onClick = { showDatePicker = true }
                    )
                    PickerTile(
                        icon = Icons.Rounded.AccessTime,
                        label = "时间",
                        value = SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(dueTime)),
                        modifier = Modifier.weight(1f),
                        onClick = { showTimePicker = true }
                    )
                }
            }

            EditSectionCard(
                icon = Icons.Rounded.Repeat,
                title = "重复",
                subtitle = "习惯类任务建议设置每天或工作日"
            ) {
                val rules = listOf(
                    RepeatRule.NONE to "不重复",
                    RepeatRule.DAILY to "每天",
                    RepeatRule.WEEKDAY to "工作日",
                    RepeatRule.WEEKLY to "每周",
                    RepeatRule.MONTHLY to "每月",
                    RepeatRule.INTERVAL to "每隔几天"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rules.forEach { (value, label) ->
                        FilterChip(
                            selected = repeatRule == value,
                            onClick = {
                                repeatRule = value
                                if (value == RepeatRule.WEEKLY && repeatDays.isBlank()) {
                                    repeatDays = "1,2,3,4,5"
                                }
                            },
                            label = { Text(label) }
                        )
                    }
                }

                if (repeatRule == RepeatRule.WEEKLY) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "选择星期几",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(7.dp))
                    val allDays = listOf(
                        1 to "一", 2 to "二", 3 to "三", 4 to "四",
                        5 to "五", 6 to "六", 7 to "日"
                    )
                    val selected = repeatDays.split(",")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.toIntOrNull() }
                        .toSet()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        allDays.forEach { (num, label) ->
                            FilterChip(
                                selected = num in selected,
                                onClick = {
                                    val next = selected.toMutableSet()
                                    if (num in next) next.remove(num) else next.add(num)
                                    repeatDays = next.sorted().joinToString(",")
                                },
                                label = { Text("周$label") }
                            )
                        }
                    }
                }

                if (repeatRule == RepeatRule.MONTHLY) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dayOfMonth.toString(),
                        onValueChange = {
                            dayOfMonth = it.toIntOrNull()?.coerceIn(1, 31) ?: 1
                        },
                        label = { Text("每月几号（1-31）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (repeatRule == RepeatRule.INTERVAL) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = repeatInterval,
                        onValueChange = { repeatInterval = it.filter { c -> c.isDigit() } },
                        label = { Text("每隔几天") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (repeatRule != RepeatRule.NONE) {
                EditSectionCard(
                    icon = Icons.Rounded.LocalFireDepartment,
                    title = "打卡记录",
                    subtitle = "已经坚持过一段时间时，可以直接修改连续天数"
                ) {
                    OutlinedTextField(
                        value = streakInput,
                        onValueChange = { streakInput = it.filter { char -> char.isDigit() } },
                        label = { Text("累计打卡天数") },
                        placeholder = { Text("例如：30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "设置后从昨天开始计算最近连续记录，今天仍可继续打卡。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            EditSectionCard(
                icon = Icons.Rounded.NotificationsActive,
                title = "提前提醒",
                subtitle = "设置提前量，避免临时手忙脚乱"
            ) {
                val presets = listOf(
                    0 to "准时",
                    5 to "提前5分钟",
                    15 to "提前15分钟",
                    30 to "提前30分钟",
                    60 to "提前1小时"
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    presets.forEach { (minutes, label) ->
                        FilterChip(
                            selected = (advance.toIntOrNull() ?: 0) == minutes,
                            onClick = { advance = minutes.toString() },
                            label = { Text(label) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = advance,
                    onValueChange = { advance = it.filter { c -> c.isDigit() } },
                    label = { Text("自定义提前分钟数（0 表示准时）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueTime)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { selectedMillis ->
                        val picked = Calendar.getInstance().apply { timeInMillis = selectedMillis }
                        val original = Calendar.getInstance().apply { timeInMillis = dueTime }
                        picked.set(Calendar.HOUR_OF_DAY, original.get(Calendar.HOUR_OF_DAY))
                        picked.set(Calendar.MINUTE, original.get(Calendar.MINUTE))
                        picked.set(Calendar.SECOND, 0)
                        picked.set(Calendar.MILLISECOND, 0)
                        dueTime = picked.timeInMillis
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        val calendar = Calendar.getInstance().apply { timeInMillis = dueTime }
        val state = rememberTimePickerState(
            initialHour = calendar.get(Calendar.HOUR_OF_DAY),
            initialMinute = calendar.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val updated = Calendar.getInstance().apply { timeInMillis = dueTime }
                    updated.set(Calendar.HOUR_OF_DAY, state.hour)
                    updated.set(Calendar.MINUTE, state.minute)
                    updated.set(Calendar.SECOND, 0)
                    updated.set(Calendar.MILLISECOND, 0)
                    dueTime = updated.timeInMillis
                    showTimePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("取消") }
            },
            text = { TimePicker(state = state) }
        )
    }
}

@Composable
private fun EditSectionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun PickerTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(9.dp))
            Column {
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    value,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

private fun isSameDay(first: Long, second: Long): Boolean =
    dayStart(first) == dayStart(second)

private fun shiftDay(time: Long, offsetDays: Int): Long = Calendar.getInstance().apply {
    timeInMillis = time
    add(Calendar.DAY_OF_YEAR, offsetDays)
}.timeInMillis

private fun dayStart(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun yesterdayStart(): Long = Calendar.getInstance().apply {
    add(Calendar.DAY_OF_YEAR, -1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun defaultDueTime(): Long {
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.HOUR_OF_DAY, 1)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)
    return calendar.timeInMillis
}













