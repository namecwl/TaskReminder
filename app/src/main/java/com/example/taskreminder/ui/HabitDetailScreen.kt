package com.example.taskreminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.automirrored.rounded.Note
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskreminder.data.HabitCheckIn
import com.example.taskreminder.data.HabitCheckInMode
import com.example.taskreminder.data.HabitStatus
import com.example.taskreminder.data.Task
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

private const val DEFAULT_HABIT_COLOR = "#4B8DF8"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    task: Task,
    records: Map<Long, HabitCheckIn>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onToggle: () -> Unit,
    onSetStatus: (dayStart: Long, status: String?, amount: Int, note: String) -> Unit,
    onRecordAmount: (dayStart: Long, amount: Int, note: String) -> Unit,
    onArchive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val accent = remember(task.habitColor) { habitColor(task.habitColor) }
    val todayStart = remember { startOfDay(System.currentTimeMillis()) }
    var selectedMonth by remember { mutableLongStateOf(monthStart(System.currentTimeMillis())) }
    var selectedDay by remember { mutableLongStateOf(todayStart) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showHeatmap by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    val todayRecord = records[todayStart]
    val todayAmount = todayRecord?.amount?.coerceAtLeast(0) ?: 0
    val doneToday = todayRecord?.status == HabitStatus.DONE
    val stats = remember(records, selectedMonth) { habitStats(records, selectedMonth) }
    val accentContent = readableOn(accent)

    if (showRecordDialog) {
        HabitRecordDialog(
            task = task,
            dayStart = selectedDay,
            existing = records[selectedDay],
            accent = accent,
            onDismiss = { showRecordDialog = false },
            onSave = { amount, note ->
                onRecordAmount(selectedDay, amount, note)
                showRecordDialog = false
            },
            onClear = {
                onSetStatus(selectedDay, null, 0, "")
                showRecordDialog = false
            },
            onFail = {
                onSetStatus(selectedDay, HabitStatus.FAILED, 0, records[selectedDay]?.note.orEmpty())
                showRecordDialog = false
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除习惯？") },
            text = { Text("所有打卡记录也会被永久删除，且无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }

    if (showHeatmap) {
        HabitHeatmapDialog(
            task = task,
            records = records,
            accent = accent,
            onDismiss = { showHeatmap = false }
        )
    }

    Scaffold(
        containerColor = accent,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    HeroIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        HeroIconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("编辑习惯") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                onClick = {
                                    menuOpen = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (task.habitArchived) "恢复习惯" else "归档习惯") },
                                leadingIcon = {
                                    Icon(
                                        if (task.habitArchived) Icons.Rounded.Restore
                                        else Icons.Rounded.Archive,
                                        null
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    onArchive(!task.habitArchived)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("删除") },
                                leadingIcon = { Icon(Icons.Rounded.Delete, null) },
                                onClick = {
                                    menuOpen = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    navigationIconContentColor = accentContent,
                    actionIconContentColor = accentContent
                )
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(accent, accent.copy(alpha = 0.93f), accent.copy(alpha = 0.82f))
                    )
                ),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                HabitHero(
                    task = task,
                    todayAmount = todayAmount,
                    doneToday = doneToday,
                    accent = accent,
                    onMainAction = {
                        if (!doneToday && task.habitCheckInMode == HabitCheckInMode.MANUAL) {
                            selectedDay = todayStart
                            showRecordDialog = true
                        } else {
                            onToggle()
                        }
                    },
                    onScrollToCalendar = {
                        scope.launch { listState.animateScrollToItem(2) }
                    }
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
                HabitStatsHeader(title = "打卡数据", onMore = { showHeatmap = true })
            }
            item {
                HabitStatsGrid(task = task, stats = stats, accent = accent)
            }
            item {
                Spacer(Modifier.height(12.dp))
                HabitCalendarCard(
                    records = records,
                    selectedMonth = selectedMonth,
                    accent = accent,
                    onPreviousMonth = { selectedMonth = shiftMonth(selectedMonth, -1) },
                    onNextMonth = { selectedMonth = shiftMonth(selectedMonth, 1) },
                    onDayClick = { dayStart ->
                        selectedDay = dayStart
                        showRecordDialog = true
                    }
                )
            }
            item {
                Spacer(Modifier.height(12.dp))
                HabitAmountChart(
                    records = records,
                    target = task.habitTarget.coerceAtLeast(1),
                    accent = accent
                )
            }
            if (task.habitLogEnabled) {
                item {
                    Spacer(Modifier.height(12.dp))
                    HabitTimeline(task = task, records = records, accent = accent)
                }
            }
            item {
                Spacer(Modifier.height(18.dp))
                Text(
                    "点击日历日期可补记或撤销；再次点击主按钮可撤销今天。",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun HabitHero(
    task: Task,
    todayAmount: Int,
    doneToday: Boolean,
    accent: Color,
    onMainAction: () -> Unit,
    onScrollToCalendar: () -> Unit
) {
    val target = task.habitTarget.coerceAtLeast(1)
    val progress = (todayAmount.toFloat() / target.toFloat()).coerceIn(0f, 1f)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(18.dp))
        Surface(
            modifier = Modifier.size(116.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.20f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(task.habitIcon.ifBlank { "✅" }, fontSize = 56.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            task.title.ifBlank { "习惯打卡" },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        if (task.note.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                task.note,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            when {
                task.habitCheckInMode == HabitCheckInMode.COMPLETE && doneToday -> "今天已完成"
                task.habitCheckInMode == HabitCheckInMode.COMPLETE -> "每天完成一次"
                else -> "$todayAmount / $target ${task.habitUnit}"
            },
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.82f)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(190.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(Color.White.copy(alpha = 0.26f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(4.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(Color.White)
            )
        }
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onMainAction,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(99.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.34f),
                contentColor = Color.White
            )
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = Color.White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        if (doneToday) Icons.AutoMirrored.Rounded.Undo else Icons.Rounded.Check,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                when {
                    doneToday -> "已完成 · 点击撤销"
                    task.habitCheckInMode == HabitCheckInMode.AUTO ->
                        "记录 +1 ${task.habitUnit}"
                    task.habitCheckInMode == HabitCheckInMode.MANUAL ->
                        "记录完成量"
                    else -> "完成打卡"
                },
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (doneToday) {
            Spacer(Modifier.height(10.dp))
            Text(
                "今天已打卡，不会再次发送提醒",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.76f)
            )
        }

        Spacer(Modifier.height(14.dp))
        IconButton(onClick = onScrollToCalendar) {
            Icon(
                Icons.Rounded.KeyboardArrowUp,
                contentDescription = "查看日历",
                tint = Color.White.copy(alpha = 0.78f),
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun HabitStatsHeader(title: String, onMore: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onMore) {
            Text("更多", color = Color.White.copy(alpha = 0.84f))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.84f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun HabitStatsGrid(task: Task, stats: HabitStats, accent: Color) {
    val items = listOf(
        StatItem("月打卡", "${stats.monthDone} 天", Icons.Rounded.CalendarMonth, accent),
        StatItem("总打卡", "${stats.totalDone} 天", Icons.Rounded.Check, accent),
        StatItem("月完成率", "${stats.monthRate}%", Icons.AutoMirrored.Rounded.TrendingUp, accent),
        StatItem("当前连续", "${task.streak} 天", Icons.Rounded.LocalFireDepartment, accent),
        StatItem("月完成量", "${stats.monthAmount} ${task.habitUnit}", Icons.Rounded.BarChart, accent),
        StatItem("总完成量", "${stats.totalAmount} ${task.habitUnit}", Icons.Rounded.GridView, accent)
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { item ->
                        StatCard(item = item, modifier = Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private data class StatItem(
    val label: String,
    val value: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val color: Color
)

@Composable
private fun StatCard(item: StatItem, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = item.color,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    item.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(5.dp))
            Text(
                item.value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HabitCalendarCard(
    records: Map<Long, HabitCheckIn>,
    selectedMonth: Long,
    accent: Color,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (Long) -> Unit
) {
    val cells = remember(selectedMonth) { monthCells(selectedMonth) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "上个月")
                }
                Text(
                    SimpleDateFormat("yyyy年M月", Locale.SIMPLIFIED_CHINESE)
                        .format(Date(selectedMonth)),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onNextMonth) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "下个月")
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                    Text(
                        label,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            cells.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { day ->
                        val record = records[day.dayStart]
                        HabitMonthCell(
                            day = day,
                            record = record,
                            accent = accent,
                            modifier = Modifier.weight(1f),
                            onClick = { if (day.inMonth) onDayClick(day.dayStart) }
                        )
                    }
                }
            }
        }
    }
}

private data class MonthCell(
    val dayStart: Long,
    val day: Int,
    val inMonth: Boolean,
    val today: Boolean
)

@Composable
private fun HabitMonthCell(
    day: MonthCell,
    record: HabitCheckIn?,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val done = record?.status == HabitStatus.DONE
    val failed = record?.status == HabitStatus.FAILED
    val progress = record?.status == HabitStatus.PROGRESS
    Box(
        modifier = modifier
            .height(45.dp)
            .padding(3.dp)
            .clip(CircleShape)
            .background(
                when {
                    done -> accent
                    failed -> MaterialTheme.colorScheme.errorContainer
                    day.today -> accent.copy(alpha = 0.14f)
                    else -> Color.Transparent
                }
            )
            .border(
                width = if (day.today && !done) 1.5.dp else 0.dp,
                color = if (day.today && !done) accent else Color.Transparent,
                shape = CircleShape
            )
            .clickable(enabled = day.inMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        when {
            done -> Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = readableOn(accent),
                modifier = Modifier.size(18.dp)
            )
            failed -> Text("×", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            progress -> Text(
                "${record?.amount ?: 0}",
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                fontWeight = FontWeight.Bold
            )
            else -> Text(
                day.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    day.inMonth -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)
                }
            )
        }
    }
}

@Composable
private fun HabitAmountChart(
    records: Map<Long, HabitCheckIn>,
    target: Int,
    accent: Color
) {
    val lastSeven = remember(records) { recentDays(records, 7) }
    val maxValue = max(target, lastSeven.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("每日完成量", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "最近 7 天",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                lastSeven.forEach { (dayStart, amount) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height((12 + 68f * amount / maxValue).roundToInt().dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (amount >= target) accent
                                    else accent.copy(alpha = 0.38f)
                                )
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            SimpleDateFormat("E", Locale.SIMPLIFIED_CHINESE).format(Date(dayStart)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitTimeline(
    task: Task,
    records: Map<Long, HabitCheckIn>,
    accent: Color
) {
    val entries = remember(records) {
        records.values
            .filter { it.status == HabitStatus.DONE || it.status == HabitStatus.FAILED || it.note.isNotBlank() }
            .sortedByDescending { it.dayStart }
            .take(12)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.Note, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text("打卡日志", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(12.dp))
            if (entries.isEmpty()) {
                Text(
                    "完成打卡后，可以在这里记录当天的心情或心得。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                entries.forEachIndexed { index, record ->
                    HabitTimelineRow(task = task, record = record, accent = accent)
                    if (index != entries.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun HabitTimelineRow(task: Task, record: HabitCheckIn, accent: Color) {
    val done = record.status == HabitStatus.DONE
    val failed = record.status == HabitStatus.FAILED
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> accent
                        failed -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.outline
                    }
                )
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    SimpleDateFormat("M月d日 E", Locale.SIMPLIFIED_CHINESE)
                        .format(Date(record.dayStart)),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(Modifier.weight(1f))
                Text(
                    when {
                        done -> "${record.amount} ${task.habitUnit}"
                        failed -> "未完成"
                        else -> "${record.amount} ${task.habitUnit}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        done -> accent
                        failed -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            if (record.note.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    record.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HabitRecordDialog(
    task: Task,
    dayStart: Long,
    existing: HabitCheckIn?,
    accent: Color,
    onDismiss: () -> Unit,
    onSave: (Int, String) -> Unit,
    onClear: () -> Unit,
    onFail: () -> Unit
) {
    var amount by remember {
        mutableStateOf((existing?.amount ?: task.habitTarget).coerceAtLeast(0).toString())
    }
    var note by remember { mutableStateOf(existing?.note.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(if (task.habitCheckInMode == HabitCheckInMode.MANUAL) "记录完成量" else "补记打卡")
                Text(
                    SimpleDateFormat("M月d日 E", Locale.SIMPLIFIED_CHINESE).format(Date(dayStart)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it.filter(Char::isDigit).ifBlank { "0" } },
                    label = { Text("完成量（目标 ${task.habitTarget} ${task.habitUnit}）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (task.habitLogEnabled) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("打卡日志（可选）") },
                        placeholder = { Text("今天做得怎么样？") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Text(
                    "达到目标后会自动标记为已完成；也可以随时清除记录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave((amount.toIntOrNull() ?: 0).coerceAtLeast(0), note) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text("保存记录") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("清除") }
                TextButton(onClick = onFail) { Text("未完成") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        }
    )
}

@Composable
private fun HabitHeatmapDialog(
    task: Task,
    records: Map<Long, HabitCheckIn>,
    accent: Color,
    onDismiss: () -> Unit
) {
    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var year by remember { mutableIntStateOf(currentYear) }
    val weeks = remember(year) { heatmapWeeks(year) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { year-- }) {
                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "上一年")
                }
                Text(
                    "$year 年",
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { if (year < currentYear) year++ }) {
                    Icon(Icons.Rounded.ChevronRight, contentDescription = "下一年")
                }
            }
        },
        text = {
            Column {
                Text(
                    "${task.habitIcon} ${task.title}",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(14.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    weeks.forEach { week ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            week.forEach { day ->
                                val amount = records[day]?.amount ?: 0
                                val intensity = (amount.toFloat() / task.habitTarget.coerceAtLeast(1))
                                    .coerceIn(0f, 1f)
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(
                                            if (day == 0L) Color.Transparent
                                            else if (amount > 0) accent.copy(alpha = 0.22f + 0.78f * intensity)
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    "颜色越深表示当天完成量越高",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        }
    )
}

@Composable
private fun HeroIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .size(42.dp)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.30f),
        contentColor = Color.White
    ) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

private data class HabitStats(
    val monthDone: Int,
    val totalDone: Int,
    val monthRate: Int,
    val monthAmount: Int,
    val totalAmount: Int
)

private fun habitStats(
    records: Map<Long, HabitCheckIn>,
    monthStart: Long
): HabitStats {
    val doneRecords = records.values.filter { it.status == HabitStatus.DONE }
    val mStart = monthStart(monthStart)
    val mEnd = shiftMonth(mStart, 1)
    val monthDoneRecords = doneRecords.filter { it.dayStart in mStart until mEnd }
    val daysInMonth = Calendar.getInstance().apply {
        timeInMillis = mStart
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
    }.get(Calendar.DAY_OF_MONTH)
    val rate = if (daysInMonth == 0) 0 else {
        (monthDoneRecords.size * 100f / daysInMonth).roundToInt()
    }
    return HabitStats(
        monthDone = monthDoneRecords.size,
        totalDone = doneRecords.size,
        monthRate = rate,
        monthAmount = monthDoneRecords.sumOf { it.amount.coerceAtLeast(1) },
        totalAmount = doneRecords.sumOf { it.amount.coerceAtLeast(1) }
    )
}

private fun recentDays(records: Map<Long, HabitCheckIn>, count: Int): List<Pair<Long, Int>> {
    val today = startOfDay(System.currentTimeMillis())
    return (count - 1 downTo 0).map { offset ->
        val day = Calendar.getInstance().apply {
            timeInMillis = today
            add(Calendar.DAY_OF_YEAR, -offset)
        }.timeInMillis
        day to (records[day]?.amount ?: 0)
    }
}

private fun monthCells(monthStartValue: Long): List<MonthCell> {
    val monthStart = monthStart(monthStartValue)
    val first = Calendar.getInstance().apply { timeInMillis = monthStart }
    val inMonth = first.get(Calendar.MONTH)
    val offset = when (first.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 6
    }
    val gridStart = Calendar.getInstance().apply {
        timeInMillis = monthStart
        add(Calendar.DAY_OF_MONTH, -offset)
    }.timeInMillis
    val today = startOfDay(System.currentTimeMillis())
    return (0 until 42).map { index ->
        val day = Calendar.getInstance().apply {
            timeInMillis = gridStart
            add(Calendar.DAY_OF_MONTH, index)
        }
        val dayStart = startOfDay(day.timeInMillis)
        MonthCell(
            dayStart = dayStart,
            day = day.get(Calendar.DAY_OF_MONTH),
            inMonth = day.get(Calendar.MONTH) == inMonth,
            today = dayStart == today
        )
    }
}

private fun heatmapWeeks(year: Int): List<List<Long>> {
    val first = Calendar.getInstance().apply {
        clear()
        set(year, Calendar.JANUARY, 1)
    }
    val offset = when (first.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        else -> 6
    }
    val cursor = Calendar.getInstance().apply {
        timeInMillis = first.timeInMillis
        add(Calendar.DAY_OF_YEAR, -offset)
    }
    val weeks = mutableListOf<List<Long>>()
    repeat(53) {
        val week = mutableListOf<Long>()
        repeat(7) {
            val inYear = cursor.get(Calendar.YEAR) == year
            week += if (inYear) startOfDay(cursor.timeInMillis) else 0L
            cursor.add(Calendar.DAY_OF_YEAR, 1)
        }
        weeks += week
    }
    return weeks
}

private fun shiftMonth(timeMillis: Long, offset: Int): Long = Calendar.getInstance().apply {
    timeInMillis = monthStart(timeMillis)
    add(Calendar.MONTH, offset)
}.timeInMillis

private fun monthStart(timeMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timeMillis
    set(Calendar.DAY_OF_MONTH, 1)
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun startOfDay(timeMillis: Long): Long = Calendar.getInstance().apply {
    timeInMillis = timeMillis
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

private fun habitColor(value: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(value))
}.getOrDefault(Color(android.graphics.Color.parseColor(DEFAULT_HABIT_COLOR)))

private fun readableOn(color: Color): Color {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return if (luminance > 0.64f) Color(0xFF1D2733) else Color.White
}
