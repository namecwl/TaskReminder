package com.example.taskreminder.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskEditScreen(
    task: Task?,
    onSave: (Task) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val isNew = task == null

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

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "新建任务" else "编辑任务") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "取消")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isBlank()) {
                                Toast.makeText(context, "请填写标题", Toast.LENGTH_SHORT).show()
                                return@TextButton
                            }
                            val newTask = (task ?: Task()).copy(
                                title = title.trim(),
                                note = note.trim(),
                                dueTime = dueTime,
                                repeatRule = repeatRule,
                                repeatInterval = repeatInterval.toIntOrNull() ?: 2,
                                repeatDaysOfWeek = repeatDays,
                                repeatDayOfMonth = dayOfMonth,
                                advanceMinutes = advance.toIntOrNull() ?: 0,
                                isCompleted = false,
                                completedAt = null,
                                enabled = true
                            )
                            onSave(newTask)
                        }
                    ) { Text("保存") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("提醒时间", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            .format(Date(dueTime))
                    )
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault())
                            .format(Date(dueTime))
                    )
                }
            }

            Text("重复", style = MaterialTheme.typography.titleMedium)
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
                        onClick = { repeatRule = value },
                        label = { Text(label) }
                    )
                }
            }

            if (repeatRule == RepeatRule.WEEKLY) {
                Text("选择星期几", style = MaterialTheme.typography.bodyMedium)
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
                                val ns = selected.toMutableSet()
                                if (num in ns) ns.remove(num) else ns.add(num)
                                repeatDays = ns.sorted().joinToString(",")
                            },
                            label = { Text("周$label") }
                        )
                    }
                }
            }

            if (repeatRule == RepeatRule.MONTHLY) {
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
                OutlinedTextField(
                    value = repeatInterval,
                    onValueChange = { repeatInterval = it.filter { c -> c.isDigit() } },
                    label = { Text("每隔几天") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = advance,
                onValueChange = { advance = it.filter { c -> c.isDigit() } },
                label = { Text("提前提醒分钟数（0 表示不提前）") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dueTime)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms ->
                        val picked = Calendar.getInstance().apply { timeInMillis = ms }
                        val orig = Calendar.getInstance().apply { timeInMillis = dueTime }
                        picked.set(Calendar.HOUR_OF_DAY, orig.get(Calendar.HOUR_OF_DAY))
                        picked.set(Calendar.MINUTE, orig.get(Calendar.MINUTE))
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
        val cal = Calendar.getInstance().apply { timeInMillis = dueTime }
        val state = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = dueTime }
                    c.set(Calendar.HOUR_OF_DAY, state.hour)
                    c.set(Calendar.MINUTE, state.minute)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    dueTime = c.timeInMillis
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

private fun defaultDueTime(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.HOUR_OF_DAY, 1)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}