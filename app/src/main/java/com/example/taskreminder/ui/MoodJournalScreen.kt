package com.example.taskreminder.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.taskreminder.data.MoodEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class MoodOption(
    val key: String,
    val emoji: String,
    val label: String,
    val colors: List<Color>
)

internal val moodOptions = listOf(
    MoodOption("happy", "😄", "开心", listOf(Color(0xFFFFB74D), Color(0xFFFF8A65))),
    MoodOption("excited", "🤩", "兴奋", listOf(Color(0xFFFF7A59), Color(0xFFFFB347))),
    MoodOption("calm", "😌", "平静", listOf(Color(0xFF5C8DFF), Color(0xFF5BC0BE))),
    MoodOption("grateful", "🥰", "感激", listOf(Color(0xFFFF8FA3), Color(0xFFFFB4A2))),
    MoodOption("hopeful", "🌱", "期待", listOf(Color(0xFF46B98A), Color(0xFF8BD3A8))),
    MoodOption("tired", "😴", "疲惫", listOf(Color(0xFF8E9AAF), Color(0xFFB8A1D9))),
    MoodOption("anxious", "😰", "焦虑", listOf(Color(0xFFF6A84A), Color(0xFFE86A70))),
    MoodOption("annoyed", "😤", "烦闷", listOf(Color(0xFFE58B52), Color(0xFFC96A6A))),
    MoodOption("angry", "😠", "生气", listOf(Color(0xFFE05252), Color(0xFFB72E4A))),
    MoodOption("low", "😔", "低落", listOf(Color(0xFF607D9B), Color(0xFF876D9E))),
    MoodOption("sad", "😢", "难过", listOf(Color(0xFF5C78A8), Color(0xFF7B8FC7))),
    MoodOption("lonely", "🌙", "孤独", listOf(Color(0xFF586B9C), Color(0xFF7B6FA8))),
    MoodOption("confused", "😕", "迷茫", listOf(Color(0xFF8994A6), Color(0xFFA79BC4))),
    MoodOption("shy", "😳", "害羞", listOf(Color(0xFFFF9FAE), Color(0xFFE7A0D0))),
    MoodOption("neutral", "😐", "一般", listOf(Color(0xFF8C99A8), Color(0xFFA6B0BC)))
)

internal fun moodOption(key: String): MoodOption =
    moodOptions.firstOrNull { it.key == key } ?: moodOptions[1]

@Composable
internal fun TodayMoodCard(
    entries: List<MoodEntry>,
    justSaved: Boolean,
    onAdd: () -> Unit,
    onViewAll: () -> Unit
) {
    val todayEntries = remember(entries) { entries.filter { isToday(it.createdAt) } }
    val latest = todayEntries.firstOrNull()
    val option = moodOption(latest?.moodKey ?: "calm")
    val gradient = Brush.linearGradient(
        colors = option.colors,
        start = Offset.Zero,
        end = Offset(1000f, 420f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "今日心情",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.84f)
                    )
                    Text(
                        if (todayEntries.isEmpty()) "记录此刻的感觉" else moodPrompt(option.key),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                }
                Text(option.emoji, style = MaterialTheme.typography.displaySmall)
            }

            AnimatedVisibility(visible = justSaved) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Text(
                        "已记录，可以继续添加",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White
                    )
                }
            }

            if (latest != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                option.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "今天 ${
                                    SimpleDateFormat("HH:mm", Locale.getDefault())
                                        .format(Date(latest.createdAt))
                                } · 共 ${todayEntries.size} 次",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }
                        if (latest.note.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                latest.note,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.92f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onAdd,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = option.colors.first()
                    )
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (todayEntries.isEmpty()) "写下心情" else "再记一次")
                }
                if (entries.isNotEmpty()) {
                    TextButton(
                        onClick = onViewAll,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("回顾", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodComposerDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onSave: (moodKey: String, note: String) -> Unit
) {
    if (!visible) return

    var moodKey by remember(visible) { mutableStateOf("calm") }
    var note by remember(visible) { mutableStateOf("") }
    val option = moodOption(moodKey)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(option.emoji, style = MaterialTheme.typography.displaySmall) },
        title = { Text("记录此刻") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "同一天可以记录多次，每次都会按时间保存。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    moodOptions.forEach { mood ->
                        Surface(
                            modifier = Modifier.clickable { moodKey = mood.key },
                            shape = RoundedCornerShape(14.dp),
                            color = if (mood.key == moodKey) {
                                mood.colors.first().copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
                            },
                            border = if (mood.key == moodKey) {
                                BorderStroke(1.dp, mood.colors.first().copy(alpha = 0.55f))
                            } else {
                                null
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mood.emoji)
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    mood.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("写几句现在的心情或发生的事…") },
                    minLines = 4,
                    maxLines = 7,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(moodKey, note)
                },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("保存记录")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MoodHistoryScreen(
    entries: List<MoodEntry>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onDelete: (MoodEntry) -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("心情回顾") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                text = { Text("记录此刻") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp)
            )
        }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🫧", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(10.dp))
                    Text("还没有心情记录", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "记录一点心情，慢慢形成自己的情绪轨迹。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 108.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                entries.groupBy { dayKey(it.createdAt) }.forEach { (_, dayEntries) ->
                    item(key = "date_${dayEntries.first().createdAt}") {
                        Text(
                            friendlyDay(dayEntries.first().createdAt),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                        )
                    }
                    items(dayEntries, key = { it.id }) { entry ->
                        MoodHistoryCard(entry = entry, onDelete = { onDelete(entry) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodHistoryCard(entry: MoodEntry, onDelete: () -> Unit) {
    val option = moodOption(entry.moodKey)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 15.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = option.colors.first().copy(alpha = 0.16f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(option.emoji, style = MaterialTheme.typography.titleLarge)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.createdAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.note.isNotBlank()) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        entry.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(38.dp)) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "删除这条记录",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun moodPrompt(key: String): String = when (key) {
    "happy" -> "今天真不错，保持这份开心"
    "excited" -> "把这份能量，变成今天的动力"
    "grateful" -> "记得把温柔和感谢说出来"
    "hopeful" -> "心里有期待，日子就有方向"
    "tired" -> "辛苦了，给自己一点休息时间"
    "anxious" -> "慢慢来，一件一件处理"
    "annoyed" -> "先深呼吸，把烦恼放轻一点"
    "angry" -> "允许生气，但别让它伤到自己"
    "low" -> "允许自己低落一会儿，明天再出发"
    "sad" -> "难过也没关系，先陪陪自己"
    "lonely" -> "一个人的时候，也值得被好好照顾"
    "confused" -> "看不清路时，就先走好眼前一步"
    "shy" -> "害羞也是可爱的一部分"
    "neutral" -> "普通的一天，也值得认真记录"
    else -> "今天感觉怎么样？"
}

private fun dayKey(time: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))

private fun friendlyDay(time: Long): String {
    val today = startOfDay(System.currentTimeMillis())
    val day = startOfDay(time)
    return when (day) {
        today -> "今天 · ${SimpleDateFormat("M月d日", Locale.SIMPLIFIED_CHINESE).format(Date(time))}"
        today - 86_400_000L -> "昨天 · ${SimpleDateFormat("M月d日", Locale.SIMPLIFIED_CHINESE).format(Date(time))}"
        else -> SimpleDateFormat("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE).format(Date(time))
    }
}

private fun isToday(time: Long): Boolean = startOfDay(time) == startOfDay(System.currentTimeMillis())

private fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis


