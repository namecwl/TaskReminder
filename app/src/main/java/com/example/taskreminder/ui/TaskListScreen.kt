package com.example.taskreminder.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.animation.core.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskreminder.alarm.OngoingReminderService
import com.example.taskreminder.data.RepeatRule
import com.example.taskreminder.data.Task
import com.example.taskreminder.util.BackupUtil
import com.example.taskreminder.util.NaturalLanguageParser
import com.example.taskreminder.util.OngoingNotifier
import com.example.taskreminder.vm.MoodViewModel
import com.example.taskreminder.vm.TaskViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * 首页。
 *
 * 界面结构参考滴答清单：
 * 1. 顶部显示日期和操作入口。
 * 2. 今日心情模块可以记录情绪和短日记。
 * 3. 快捷输入支持自然语言添加任务。
 * 4. 任务按今天、打卡、计划和已完成分组展示。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    vm: TaskViewModel,
    onEdit: (Task) -> Unit,
    onNew: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val moodVm: MoodViewModel = viewModel()
    val moodEntries by moodVm.entries.collectAsStateWithLifecycle()
    var showMoodHistory by remember { mutableStateOf(false) }
    var showMoodComposer by remember { mutableStateOf(false) }
    var moodJustSaved by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var showCompleted by remember { mutableStateOf(false) }
    var quickInput by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(moodJustSaved) {
        if (moodJustSaved) {
            delay(1_600L)
            moodJustSaved = false
        }
    }

    if (showMoodHistory) {
        MoodHistoryScreen(
            entries = moodEntries,
            onBack = { showMoodHistory = false },
            onAdd = { showMoodComposer = true },
            onDelete = moodVm::delete
        )
        MoodComposerDialog(
            visible = showMoodComposer,
            onDismiss = { showMoodComposer = false },
            onSave = { moodKey, note ->
                moodVm.add(moodKey, note)
                showMoodComposer = false
                moodJustSaved = true
            }
        )
        return
    }

    MoodComposerDialog(
        visible = showMoodComposer,
        onDismiss = { showMoodComposer = false },
        onSave = { moodKey, note ->
            moodVm.add(moodKey, note)
            showMoodComposer = false
            moodJustSaved = true
        }
    )
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000L)
            now = System.currentTimeMillis()
        }
    }

    LaunchedEffect(tasks) {
        OngoingNotifier.update(context, tasks)
        OngoingReminderService.start(context)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                BackupUtil.writeToUri(context, uri, BackupUtil.toJson(tasks))
                Toast.makeText(context, "已导出 ${tasks.size} 条任务", Toast.LENGTH_SHORT).show()
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
                val importList = BackupUtil.fromJson(BackupUtil.readFromUri(context, uri))
                vm.importTasks(importList)
                Toast.makeText(context, "已导入 ${importList.size} 条任务", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "导入失败：${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    val groups = remember(tasks, now) { groupTasks(tasks, now) }
    val taskFromParsed: (NaturalLanguageParser.Parsed) -> Task = { parsed ->
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
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "我的任务",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            friendlyToday(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showCompleted = !showCompleted }) {
                        Icon(
                            if (showCompleted) Icons.Rounded.VisibilityOff
                            else Icons.Rounded.Visibility,
                            contentDescription = if (showCompleted) "隐藏已完成" else "显示已完成",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "更多",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("通知与后台设置") },
                                leadingIcon = { Icon(Icons.Rounded.NotificationsActive, null) },
                                onClick = {
                                    menuOpen = false
                                    onOpenSettings()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("导出备份") },
                                leadingIcon = { Icon(Icons.Rounded.Download, null) },
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
                                leadingIcon = { Icon(Icons.Rounded.UploadFile, null) },
                                onClick = {
                                    menuOpen = false
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("清理已完成") },
                                leadingIcon = { Icon(Icons.Rounded.CleaningServices, null) },
                                onClick = {
                                    menuOpen = false
                                    vm.clearCompleted()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNew,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("新建任务") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 今日概览和快捷输入位于列表顶部，滚动时与任务卡片保持统一节奏。
            item {
                TodayMoodCard(
                    entries = moodEntries,
                    justSaved = moodJustSaved,
                    onAdd = { showMoodComposer = true },
                    onViewAll = { showMoodHistory = true }
                )
            }

            item {
                QuickAddCard(
                    value = quickInput,
                    onValueChange = { quickInput = it },
                    onAdd = { parsed ->
                        vm.save(taskFromParsed(parsed))
                        quickInput = ""
                    },
                    onDetails = { parsed ->
                        onEdit(taskFromParsed(parsed))
                        quickInput = ""
                    }
                )
            }

            item {
                SectionHeader(
                    title = "今天",
                    subtitle = "今天到期和已经逾期的任务",
                    count = groups.today.size,
                    accent = MaterialTheme.colorScheme.primary,
                    onAdd = onNew
                )
            }
            if (groups.today.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.WbSunny,
                        title = "今天还没有安排",
                        subtitle = "点击快捷添加，或输入「今天十点洗碗」"
                    )
                }
            } else {
                items(groups.today, key = { "today_${it.id}" }) { task ->
                    TaskRow(
                        task = task,
                        now = now,
                        isHabit = false,
                        accent = if (!task.isCompleted && task.dueTime < now) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        onClick = { onEdit(task) },
                        onToggle = { vm.toggleComplete(task) },
                        onDelete = { vm.delete(task) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "打卡",
                    subtitle = "每天重复，完成后保留连续记录",
                    count = groups.habits.size,
                    accent = MaterialTheme.colorScheme.tertiary,
                    onAdd = onNew
                )
            }
            if (groups.habits.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Repeat,
                        title = "还没有习惯打卡",
                        subtitle = "输入「每天八点吃药」即可创建"
                    )
                }
            } else {
                items(groups.habits, key = { "habit_${it.id}" }) { task ->
                    HabitRow(
                        task = task,
                        now = now,
                        onClick = { onEdit(task) },
                        onToggle = { vm.toggleComplete(task) },
                        onDelete = { vm.delete(task) }
                    )
                }
            }

            item {
                SectionHeader(
                    title = "计划",
                    subtitle = "未来几天要完成的事情",
                    count = groups.future.size,
                    accent = MaterialTheme.colorScheme.secondary,
                    onAdd = onNew
                )
            }
            if (groups.future.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "未来暂时没有计划",
                        subtitle = "输入「明天下午三点开会」快速安排"
                    )
                }
            } else {
                items(groups.future, key = { "plan_${it.id}" }) { task ->
                    TaskRow(
                        task = task,
                        now = now,
                        isHabit = false,
                        accent = MaterialTheme.colorScheme.secondary,
                        showCountdown = true,
                        onClick = { onEdit(task) },
                        onToggle = { vm.toggleComplete(task) },
                        onDelete = { vm.delete(task) }
                    )
                }
            }

            if (showCompleted && groups.completed.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "已完成",
                        subtitle = "已经收尾的任务",
                        count = groups.completed.size,
                        accent = MaterialTheme.colorScheme.onSurfaceVariant,
                        onAdd = null
                    )
                }
                items(groups.completed, key = { "done_${it.id}" }) { task ->
                    TaskRow(
                        task = task,
                        now = now,
                        isHabit = false,
                        accent = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { onEdit(task) },
                        onToggle = { vm.toggleComplete(task) },
                        onDelete = { vm.delete(task) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickAddCard(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: (NaturalLanguageParser.Parsed) -> Unit,
    onDetails: (NaturalLanguageParser.Parsed) -> Unit
) {
    val preview = remember(value) {
        value.trim().takeIf { it.isNotEmpty() }?.let { NaturalLanguageParser.parse(it) }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 5.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
    ) {
        Column {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "一句话记一下，例如「晚上8点去吃饭」",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            preview?.takeIf { it.title.isNotBlank() }?.let(onAdd)
                        }
                    ),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    )
                )
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            enabled = preview?.title?.isNotBlank() == true,
                            onClick = { preview?.let(onAdd) }
                        ),
                    shape = CircleShape,
                    color = if (preview?.title?.isNotBlank() == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Send,
                            contentDescription = "添加",
                            tint = if (preview?.title?.isNotBlank() == true) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (preview != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 44.dp, end = 12.dp, bottom = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "将添加：${preview.title}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                formatParsedPreview(preview),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                            )
                        }
                        TextButton(onClick = { onDetails(preview) }) {
                            Icon(
                                Icons.Rounded.Tune,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("调整")
                        }
                    }
                }
            }
        }
    }
}
@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    count: Int,
    accent: Color,
    onAdd: (() -> Unit)?
) {
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 2.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Spacer(Modifier.width(9.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = CircleShape,
                color = accent.copy(alpha = 0.12f)
            ) {
                Text(
                    count.toString(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
            Spacer(Modifier.weight(1f))
            if (onAdd != null) {
                IconButton(onClick = onAdd, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Rounded.Add,
                        contentDescription = "添加$title",
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Text(
            subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 18.dp, top = 1.dp)
        )
    }
}

@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
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
    }
}

@Composable
private fun HabitRow(
    task: Task,
    now: Long,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val todayStart = remember(now) { startOfDay(now) }
    val checked = task.lastCompletedDay == todayStart
    val streak = task.streak.coerceAtLeast(0)
    val nextMilestone = habitNextMilestone(streak)
    val targetProgress = (streak.toFloat() / nextMilestone.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "habitRing"
    )
    var menuOpen by remember { mutableStateOf(false) }
    val accent = MaterialTheme.colorScheme.tertiary
    val days = remember(task.lastCompletedDay, task.streak, now) { habitDays(task, now) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.58f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 15.dp, vertical = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 5.dp.toPx()
                        drawArc(
                            color = accent.copy(alpha = 0.14f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = accent,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedProgress,
                            useCenter = false,
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = if (checked) accent else accent.copy(alpha = 0.12f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (checked) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = "取消今日打卡",
                                    tint = Color.White,
                                    modifier = Modifier.size(21.dp)
                                )
                            } else {
                                Text(
                                    streak.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = accent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick)
                ) {
                    Text(
                        task.title.ifBlank { "习惯打卡" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "连续 $streak 天 · ${habitLevel(streak)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        days.forEach { day ->
                            HabitDayDot(day = day, accent = accent)
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))

                Box {
                    IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "习惯操作",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("编辑与修改天数") },
                            leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                            onClick = {
                                menuOpen = false
                                onClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    habitMilestoneText(streak, nextMilestone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(visible = checked) {
                    Text(
                        todayCheckedLabel(streak),
                        style = MaterialTheme.typography.labelMedium,
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

private data class HabitDayState(
    val label: String,
    val done: Boolean,
    val today: Boolean
)

@Composable
private fun HabitDayDot(day: HabitDayState, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(19.dp)
                .clip(CircleShape)
                .background(if (day.done) accent else Color.Transparent)
                .border(
                    width = 1.5.dp,
                    color = when {
                        day.done -> accent
                        day.today -> accent.copy(alpha = 0.72f)
                        else -> MaterialTheme.colorScheme.outlineVariant
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (day.done) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            day.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (day.today) accent else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun habitDays(task: Task, now: Long): List<HabitDayState> {
    val today = startOfDay(now)
    return (6 downTo 0).map { offset ->
        val day = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -offset)
        }.timeInMillis
        val done = if (task.lastCompletedDay > 0 && task.streak > 0) {
            val distance = ((task.lastCompletedDay - day) / 86_400_000L).toInt()
            day <= task.lastCompletedDay && distance in 0 until task.streak
        } else {
            false
        }
        val weekday = Calendar.getInstance().apply { timeInMillis = day }
            .get(Calendar.DAY_OF_WEEK)
        HabitDayState(
            label = when (weekday) {
                Calendar.MONDAY -> "一"
                Calendar.TUESDAY -> "二"
                Calendar.WEDNESDAY -> "三"
                Calendar.THURSDAY -> "四"
                Calendar.FRIDAY -> "五"
                Calendar.SATURDAY -> "六"
                else -> "日"
            },
            done = done,
            today = offset == 0
        )
    }
}

private fun habitNextMilestone(streak: Int): Int {
    val milestones = listOf(3, 7, 14, 30, 60, 100)
    return milestones.firstOrNull { it > streak } ?: (((streak / 50) + 1) * 50)
}

private fun habitLevel(streak: Int): String = when {
    streak >= 100 -> "习惯之神"
    streak >= 60 -> "传奇习惯"
    streak >= 30 -> "自律大师"
    streak >= 14 -> "习惯达人"
    streak >= 7 -> "自律新星"
    streak >= 3 -> "坚持者"
    streak >= 1 -> "小火苗"
    else -> "今天开始"
}

private fun habitMilestoneText(streak: Int, nextMilestone: Int): String =
    if (streak == 0) {
        "完成一次，点亮连续记录"
    } else {
        "距离 $nextMilestone 天成就还差 ${(nextMilestone - streak).coerceAtLeast(1)} 天"
    }

private fun todayCheckedLabel(streak: Int): String =
    if (streak <= 1) "今天已完成" else "今日完成 · 连续 $streak 天"
@Composable
private fun TaskRow(
    task: Task,
    now: Long,
    isHabit: Boolean,
    accent: Color,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    showCountdown: Boolean = false
) {
    val todayStart = remember(now) { startOfDay(now) }
    val checked = if (isHabit) task.lastCompletedDay == todayStart else task.isCompleted
    val overdue = !isHabit && !task.isCompleted && task.dueTime < now
    val meta = formatTaskMeta(task, now, isHabit, showCountdown)
    val cardAlpha = if (checked) 0.62f else 1f

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            if (overdue) MaterialTheme.colorScheme.error.copy(alpha = 0.28f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 2.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 7.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(25.dp)
                        .clip(CircleShape)
                        .background(if (checked) accent else Color.Transparent)
                        .border(
                            width = 2.dp,
                            color = if (checked) accent else accent.copy(alpha = 0.55f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (checked) {
                        Icon(
                            Icons.Rounded.Check,
                            contentDescription = "取消完成",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        task.title.ifBlank { "未命名任务" },
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (checked) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (checked) TextDecoration.LineThrough else null
                    )
                    if (isHabit && task.streak > 0) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(2.dp))
                                Text(
                                    task.streak.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isHabit) Icons.Rounded.Repeat else Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        meta,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (overdue) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (task.note.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        task.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box {
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "任务操作",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.62f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = {
                            menuOpen = false
                            onClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除") },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

private fun formatParsedPreview(parsed: NaturalLanguageParser.Parsed): String {
    val todayStart = startOfDay(System.currentTimeMillis())
    val dueStart = startOfDay(parsed.dueTime)
    val dayLabel = when (dueStart) {
        todayStart -> "今天"
        todayStart + 86_400_000L -> "明天"
        todayStart + 2 * 86_400_000L -> "后天"
        else -> SimpleDateFormat("M月d日", Locale.SIMPLIFIED_CHINESE).format(Date(parsed.dueTime))
    }
    val timeLabel = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(parsed.dueTime))
    val repeatLabel = when (parsed.repeatRule) {
        RepeatRule.DAILY -> " · 每天重复"
        RepeatRule.WEEKDAY -> " · 工作日重复"
        RepeatRule.WEEKLY -> " · 每周重复"
        RepeatRule.MONTHLY -> " · 每月${parsed.repeatDayOfMonth}号"
        RepeatRule.INTERVAL -> " · 每隔${parsed.repeatInterval}天"
        else -> ""
    }
    return "$dayLabel $timeLabel$repeatLabel"
}

private fun friendlyToday(): String {
    val formatter = SimpleDateFormat("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE)
    return "今天 · ${formatter.format(Date())}"
}

private fun formatTaskMeta(
    task: Task,
    now: Long,
    isHabit: Boolean,
    showCountdown: Boolean
): String {
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(task.dueTime))
    val repeatLabel = when (task.repeatRule) {
        RepeatRule.NONE -> ""
        RepeatRule.DAILY -> "每天"
        RepeatRule.WEEKDAY -> "工作日"
        RepeatRule.WEEKLY -> {
            val days = weekDaysLabel(task.repeatDaysOfWeek)
            if (days.isBlank()) "每周" else "每周$days"
        }
        RepeatRule.MONTHLY -> "每月${task.repeatDayOfMonth}号"
        RepeatRule.INTERVAL -> "每隔${task.repeatInterval}天"
        else -> ""
    }
    if (isHabit) {
        return "${repeatLabel.ifBlank { "每天" }} · $time"
    }
    val timePart = when {
        showCountdown -> "$time · ${formatCountdown(task.dueTime - now)}"
        task.dueTime < now && !task.isCompleted -> "已逾期 · $time"
        else -> time
    }
    val repeatPart = if (repeatLabel.isBlank()) "" else " · $repeatLabel"
    val advancePart = if (task.advanceMinutes > 0) " · 提前${task.advanceMinutes}分钟" else ""
    return timePart + repeatPart + advancePart
}

private fun weekDaysLabel(csv: String): String {
    val map = mapOf(
        "1" to "一", "2" to "二", "3" to "三", "4" to "四",
        "5" to "五", "6" to "六", "7" to "日"
    )
    return csv.split(",").filter { it.isNotBlank() }
        .mapNotNull { map[it] }
        .joinToString("、")
}

private fun formatCountdown(diffMs: Long): String {
    if (diffMs <= 0) return "已到期"
    val totalMin = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    val days = totalMin / (60 * 24)
    val hours = (totalMin % (60 * 24)) / 60
    val mins = totalMin % 60
    return when {
        days > 0 -> "还有${days}天${hours}小时"
        hours > 0 -> "还有${hours}小时${mins}分"
        mins > 0 -> "还有${mins}分"
        else -> "不到1分钟"
    }
}

private fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private data class Groups(
    val habits: List<Task>,
    val today: List<Task>,
    val future: List<Task>,
    val completed: List<Task>
)

private fun groupTasks(tasks: List<Task>, now: Long): Groups {
    val todayStart = startOfDay(now)
    val tomorrowStart = todayStart + 86_400_000L

    val completed = tasks.filter { it.isCompleted }
    val active = tasks.filter { !it.isCompleted && it.enabled }

    val habits = active.filter { it.repeatRule != RepeatRule.NONE }
        .sortedBy { it.dueTime }
    val nonRepeat = active.filter { it.repeatRule == RepeatRule.NONE }
    val today = nonRepeat.filter { it.dueTime < tomorrowStart }.sortedBy { it.dueTime }
    val future = nonRepeat.filter { it.dueTime >= tomorrowStart }.sortedBy { it.dueTime }

    return Groups(habits, today, future, completed)
}

































