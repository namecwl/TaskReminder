package com.example.taskreminder.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.taskreminder.util.InteractionCopy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

internal data class MoodCategory(val key: String, val emoji: String, val label: String, val subtitle: String, val colors: List<Color>)
internal data class MoodOption(val key: String, val categoryKey: String, val emoji: String, val label: String, val colors: List<Color>)
internal data class MoodFeedback(val title: String, val message: String, val prompt: String, val option: MoodOption)
internal enum class MoodTimeFilter(val label: String) { TODAY("今天"), WEEK("本周"), MONTH("本月"), ALL("全部") }

internal val moodCategories = listOf(
    MoodCategory("emotion", "🌤️", "心情", "记录此刻真实的感受", listOf(Color(0xFF5C8DFF), Color(0xFF7C6EE6))),
    MoodCategory("insight", "💡", "想通", "把突然明白的事留下来", listOf(Color(0xFFF2A93B), Color(0xFFE77852))),
    MoodCategory("decision", "🚩", "决心", "记住决定做出的改变", listOf(Color(0xFFE85D75), Color(0xFF9B5DE5))),
    MoodCategory("growth", "🌱", "成长", "看见微小但真实的进步", listOf(Color(0xFF35A67B), Color(0xFF56B4E9)))
)

internal val moodOptions = listOf(
    MoodOption("happy", "emotion", "😄", "开心", listOf(Color(0xFFFFB74D), Color(0xFFFF8A65))),
    MoodOption("excited", "emotion", "🤩", "兴奋", listOf(Color(0xFFFF7A59), Color(0xFFFFB347))),
    MoodOption("calm", "emotion", "😌", "平静", listOf(Color(0xFF5C8DFF), Color(0xFF5BC0BE))),
    MoodOption("grateful", "emotion", "🥰", "感激", listOf(Color(0xFFFF8FA3), Color(0xFFFFB4A2))),
    MoodOption("hopeful", "emotion", "🌱", "期待", listOf(Color(0xFF46B98A), Color(0xFF8BD3A8))),
    MoodOption("tired", "emotion", "😴", "疲惫", listOf(Color(0xFF8E9AAF), Color(0xFFB8A1D9))),
    MoodOption("anxious", "emotion", "😰", "焦虑", listOf(Color(0xFFF6A84A), Color(0xFFE86A70))),
    MoodOption("annoyed", "emotion", "😤", "烦闷", listOf(Color(0xFFE58B52), Color(0xFFC96A6A))),
    MoodOption("angry", "emotion", "😠", "生气", listOf(Color(0xFFE05252), Color(0xFFB72E4A))),
    MoodOption("low", "emotion", "😔", "低落", listOf(Color(0xFF607D9B), Color(0xFF876D9E))),
    MoodOption("sad", "emotion", "😢", "难过", listOf(Color(0xFF5C78A8), Color(0xFF7B8FC7))),
    MoodOption("lonely", "emotion", "🌙", "孤独", listOf(Color(0xFF586B9C), Color(0xFF7B6FA8))),
    MoodOption("confused", "emotion", "😕", "迷茫", listOf(Color(0xFF8994A6), Color(0xFFA79BC4))),
    MoodOption("shy", "emotion", "😳", "害羞", listOf(Color(0xFFFF9FAE), Color(0xFFE7A0D0))),
    MoodOption("neutral", "emotion", "😐", "一般", listOf(Color(0xFF8C99A8), Color(0xFFA6B0BC))),
    MoodOption("insight_clarity", "insight", "💡", "想通了", listOf(Color(0xFFF6B84A), Color(0xFFE98052))),
    MoodOption("insight_perspective", "insight", "🔭", "换个角度", listOf(Color(0xFF6C8DE8), Color(0xFF8C78D7))),
    MoodOption("insight_acceptance", "insight", "🫶", "接纳自己", listOf(Color(0xFFF08FA7), Color(0xFFF4B183))),
    MoodOption("insight_release", "insight", "🍃", "放下一点", listOf(Color(0xFF58A98B), Color(0xFF86C79B))),
    MoodOption("decision_action", "decision", "⚡", "决定行动", listOf(Color(0xFFF05D6F), Color(0xFFE28A4A))),
    MoodOption("decision_goal", "decision", "🎯", "定下目标", listOf(Color(0xFFE85D75), Color(0xFFB65DD8))),
    MoodOption("decision_boundary", "decision", "🛡️", "建立边界", listOf(Color(0xFF8467D7), Color(0xFF5C78C9))),
    MoodOption("decision_change", "decision", "🔄", "开始改变", listOf(Color(0xFFD45D91), Color(0xFF8B62C9))),
    MoodOption("growth_progress", "growth", "📈", "有进步", listOf(Color(0xFF35A67B), Color(0xFF63C68E))),
    MoodOption("growth_habit", "growth", "🧱", "坚持住了", listOf(Color(0xFF3F9E86), Color(0xFF6BBFA3))),
    MoodOption("growth_focus", "growth", "🎧", "找回专注", listOf(Color(0xFF4B83C3), Color(0xFF65B7B1))),
    MoodOption("growth_brave", "growth", "🏅", "我做到了", listOf(Color(0xFFE1A13A), Color(0xFFE66D55)))
)

internal fun moodCategory(key: String) = moodCategories.firstOrNull { it.key == key } ?: moodCategories.first()
internal fun moodCategoryForOption(option: MoodOption) = moodCategory(option.categoryKey)
internal fun moodOptionsForCategory(key: String) = moodOptions.filter { it.categoryKey == key }
internal fun moodOption(key: String) = moodOptions.firstOrNull { it.key == key } ?: moodOptions.firstOrNull { it.key == "neutral" } ?: moodOptions.first()

internal fun buildMoodFeedback(key: String, note: String): MoodFeedback {
    val option = moodOption(key)
    val category = moodCategoryForOption(option)
    val seed = abs(key.hashCode() * 31 + note.length * 17)
    val message = when (category.key) {
        "insight" -> composeFeedback(insightOpeners, insightReflections, seed)
        "decision" -> composeFeedback(decisionOpeners, decisionReflections, seed)
        "growth" -> composeFeedback(growthOpeners, growthReflections, seed)
        else -> composeFeedback(emotionOpeners, emotionReflections, seed)
    }
    val prompt = when (category.key) {
        "insight" -> "如果愿意，明天再回来看看：这个念头有没有带来新的行动？"
        "decision" -> "真正的改变不需要轰轰烈烈，先把明天能做到的第一步写下来。"
        "growth" -> "进步不一定要很大，今天比昨天多走一步，就已经算数。"
        else -> "情绪没有好坏，它只是在提醒你，此刻需要被看见。"
    }
    return MoodFeedback(option.label + " · 已被好好收下", message, prompt, option)
}

private fun composeFeedback(openers: List<String>, reflections: List<String>, seed: Int): String {
    return openers[seed % openers.size] + "。" + reflections[(seed / openers.size) % reflections.size] + "。"
}

private val emotionOpeners = listOf("这份感受已经被认真听见","谢谢你愿意停下来照顾此刻的自己","情绪被写下来以后，就不再只能压在心里","这一刻的你，值得被温柔对待","无论开心还是难过，它都真实而有分量","你没有否定自己，这本身就很勇敢","今天又多认识了自己一点点","这份心情已经被安全地保存")
private val emotionReflections = listOf("或许不必马上解决，先承认它存在就很好","它也许在提醒你，某个需要一直被忽略了","等情绪缓一缓，再决定下一步也不迟","你可以一边不完美，一边继续向前","照顾情绪不是浪费时间，而是在积蓄力量","有些感受不用解释，也值得被尊重","你不是情绪本身，你只是正在经历它","把这一刻留住，未来会更懂当时的自己")
private val insightOpeners = listOf("这个念头被你抓住了","想通的瞬间值得认真记下","你刚刚看见了一个新的角度","这份理解会慢慢变成你的力量","有些事情一旦想明白，重量就会变","这不是普通的一句话，而是内心的转折点","你正在把经历变成属于自己的答案","看见问题本身，就是改变的开始")
private val insightReflections = listOf("以后遇到相似的事，你会更知道如何回应","答案不必一次完整，能看清一点就很好","把它带到下一次行动里，领悟才会扎根","允许自己慢慢消化，不必立刻变得通透","今天的理解，也许正是明天需要的提醒","你已经在用更成熟的眼光看待这件事","不再责怪当时的自己，也是一种成长","真正的答案往往安静，却足够坚定")
private val decisionOpeners = listOf("这个决定已经被郑重记住","你正在为自己选择新的方向","愿意下定决心的那一刻，改变就开始了","这不是冲动，而是你认真听见了内心","你已经迈出了很重要的一步","把决定写下来，它会变得更有力量","你在为未来的自己负责","今天的决心，会成为明天的起点")
private val decisionReflections = listOf("先做最小的一步，让行动替你累积信心","如果中途反复，也不代表决定失效，只需要重新回来","把目标写具体一点，明天会更容易开始","不必等待状态完美，先行动再调整","允许自己缓慢，但不要偷偷放弃","真正重要的是持续选择，而不是一次用力过猛","计划越简单，越容易在疲惫时继续","未来的你会感谢今天这个认真的决定")
private val growthOpeners = listOf("这份进步值得被你看见","你已经在成为更好的自己","微小改变被坚持下来，就不再微小","今天这一点点，正在慢慢累积","你做到了，不需要轻描淡写","成长常常发生在别人看不见的地方","这次你没有辜负自己的努力","每一小步都在拓宽你的边界")
private val growthReflections = listOf("继续保持节奏，不必急着一下子走很远","累的时候可以放慢，但别否定已经走过的路","把做得好的地方记下来，下次会更容易重复","成长不是直线，偶尔停顿也算前进的一部分","你不需要和别人比较，只需和昨天的自己握手","真正的自信，就是一次次做到后长出来的","允许偶尔失败，它不会抹掉今天的进步","哪怕只改变一点点，也足以证明你可以")

@Composable
internal fun TodayMoodCard(entries: List<MoodEntry>, savedFeedback: MoodFeedback?, onAdd: () -> Unit, onViewAll: () -> Unit) {
    val todayEntries = remember(entries) { entries.filter { isToday(it.createdAt) } }
    val latest = todayEntries.firstOrNull()
    val option = moodOption(latest?.moodKey ?: "calm")
    val category = moodCategoryForOption(option)
    val gradient = Brush.linearGradient(option.colors, start = Offset.Zero, end = Offset(1000f, 420f))
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(gradient).padding(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("今日记录", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.84f))
                    Text(if (todayEntries.isEmpty()) InteractionCopy.moodCardPrompt() else moodPrompt(option.key), style = MaterialTheme.typography.headlineSmall, color = Color.White)
                }
                Text(option.emoji, style = MaterialTheme.typography.displaySmall)
            }
            AnimatedVisibility(visible = savedFeedback != null) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f)) {
                    Text(savedFeedback?.title ?: InteractionCopy.moodSavedFeedback(), modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium, color = Color.White)
                }
            }
            if (latest != null) {
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp), color = Color.White.copy(alpha = 0.16f)) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(category.emoji + " " + category.label + " · " + option.label, style = MaterialTheme.typography.labelLarge, color = Color.White)
                            Spacer(Modifier.weight(1f))
                            Text("今天 " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(latest.createdAt)) + " · 共 " + todayEntries.size + " 次", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                        }
                        if (latest.note.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(latest.note, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.92f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onAdd, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = option.colors.first())) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(if (todayEntries.isEmpty()) "记录此刻" else "继续记录")
                }
                if (entries.isNotEmpty()) {
                    TextButton(onClick = onViewAll, modifier = Modifier.height(40.dp)) {
                        Icon(Icons.Rounded.History, null, tint = Color.White, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("回顾", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodComposerDialog(visible: Boolean, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    if (!visible) return
    var categoryKey by remember(visible) { mutableStateOf(moodCategories.first().key) }
    var moodKey by remember(visible) { mutableStateOf("calm") }
    var note by remember(visible) { mutableStateOf("") }
    val category = moodCategory(categoryKey)
    val options = moodOptionsForCategory(categoryKey)
    val option = moodOption(moodKey).takeIf { it.categoryKey == categoryKey } ?: options.first()
    LaunchedEffect(categoryKey, visible) { if (moodOption(moodKey).categoryKey != categoryKey) moodKey = options.first().key }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(category.emoji, style = MaterialTheme.typography.headlineSmall) },
        title = { Text("记录此刻") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("今天不只有情绪，也可以记录想通的事和做下的决定。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    moodCategories.forEach { item -> FilterPill(item.emoji + " " + item.label, item.key == categoryKey, item.colors.first()) { categoryKey = item.key } }
                }
                Text(category.subtitle, style = MaterialTheme.typography.titleMedium, color = category.colors.first())
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    options.forEach { mood -> FilterPill(mood.emoji + " " + mood.label, mood.key == option.key, mood.colors.first()) { moodKey = mood.key } }
                }
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), placeholder = { Text(moodNotePlaceholder(option)) }, minLines = 4, maxLines = 8, shape = RoundedCornerShape(16.dp))
                Text("不知道怎么开头？可以点一句", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    moodNotePrompts(category.key).forEach { prompt -> FilterPill(prompt, false, category.colors.first()) { note = if (note.isBlank()) prompt + " " else note + "\n" + prompt + " " } }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(option.key, note) }, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = option.colors.first(), contentColor = Color.White)) { Text("保存记录") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
internal fun MoodSavedFeedbackDialog(feedback: MoodFeedback?, onDismiss: () -> Unit) {
    if (feedback == null) return
    val category = moodCategoryForOption(feedback.option)
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Text(feedback.option.emoji, style = MaterialTheme.typography.displaySmall) },
        title = { Text(feedback.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Brush.linearGradient(feedback.option.colors, start = Offset.Zero, end = Offset(900f, 320f))).padding(16.dp)) {
                    Text(feedback.message, style = MaterialTheme.typography.bodyLarge, color = Color.White, fontWeight = FontWeight.Medium)
                }
                Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = category.colors.first().copy(alpha = 0.10f)) {
                    Text(feedback.prompt, Modifier.padding(13.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss, shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = feedback.option.colors.first(), contentColor = Color.White)) { Text("收下这句话") } }
    )
}

private fun moodNotePlaceholder(option: MoodOption) = when (option.categoryKey) { "insight" -> "我刚刚意识到…"; "decision" -> "我决定从今天开始…"; "growth" -> "今天我做到了…"; else -> "写几句现在的心情或发生的事…" }
private fun moodNotePrompts(key: String) = when (key) { "insight" -> listOf("我意识到","换个角度看","其实我可以","这次我学到了"); "decision" -> listOf("我决定","从今天开始","下次我会","第一步是"); "growth" -> listOf("我做到了","今天有进步","我坚持住了","谢谢自己"); else -> listOf("我感到","因为发生了","我想对自己说","此刻需要的是") }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun MoodHistoryScreen(entries: List<MoodEntry>, onBack: () -> Unit, onAdd: () -> Unit, onUpdate: (MoodEntry) -> Unit, onDelete: (MoodEntry) -> Unit) {
    BackHandler(onBack = onBack)
    var selectedCategoryKey by remember { mutableStateOf<String?>(null) }
    var timeFilter by remember { mutableStateOf(MoodTimeFilter.ALL) }
    var editingEntry by remember { mutableStateOf<MoodEntry?>(null) }
    val filtered = remember(entries, selectedCategoryKey, timeFilter) {
        entries.filter { e -> (selectedCategoryKey == null || moodOption(e.moodKey).categoryKey == selectedCategoryKey) && withinTimeFilter(e.createdAt, timeFilter) }
    }
    editingEntry?.let { entry -> MoodEntryDetailDialog(entry, { editingEntry = null }, { onUpdate(it); editingEntry = null }, { onDelete(entry); editingEntry = null }) }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TopAppBar(title = { Text("记录回顾") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "返回") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)) },
        floatingActionButton = { ExtendedFloatingActionButton(onClick = onAdd, icon = { Icon(Icons.Rounded.AutoAwesome, null) }, text = { Text("记录此刻") }, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = RoundedCornerShape(18.dp)) }
    ) { padding ->
        if (entries.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("🫧", style = MaterialTheme.typography.displaySmall); Spacer(Modifier.height(10.dp)); Text("还没有记录", style = MaterialTheme.typography.titleMedium) } }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { MoodFilterCard(entries, selectedCategoryKey, timeFilter, { selectedCategoryKey = it }, { timeFilter = it }) }
                if (filtered.isEmpty()) item { EmptyHistoryCard() }
                else filtered.groupBy { dayKey(it.createdAt) }.forEach { (_, dayEntries) ->
                    item { Text(friendlyDay(dayEntries.first().createdAt), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp)) }
                    items(dayEntries, key = { it.id }) { entry -> MoodHistoryCard(entry) { editingEntry = entry } }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodFilterCard(entries: List<MoodEntry>, selectedCategoryKey: String?, timeFilter: MoodTimeFilter, onCategory: (String?) -> Unit, onTime: (MoodTimeFilter) -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))) {
        Column(Modifier.padding(16.dp)) {
            Text("按类型看", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterPill("全部 " + entries.size, selectedCategoryKey == null, MaterialTheme.colorScheme.primary) { onCategory(null) }
                moodCategories.forEach { cat -> FilterPill(cat.emoji + " " + cat.label + " " + entries.count { moodOption(it.moodKey).categoryKey == cat.key }, selectedCategoryKey == cat.key, cat.colors.first()) { onCategory(cat.key) } }
            }
            Spacer(Modifier.height(16.dp)); Text("按时间看", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { MoodTimeFilter.entries.forEach { item -> FilterPill(item.label, timeFilter == item, MaterialTheme.colorScheme.primary) { onTime(item) } } }
        }
    }
}

@Composable
private fun FilterPill(text: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Surface(Modifier.clickable(onClick = onClick), shape = RoundedCornerShape(99.dp), color = if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f), border = if (selected) BorderStroke(1.dp, color.copy(alpha = 0.45f)) else null) {
        Text(text, Modifier.padding(horizontal = 11.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium, color = if (selected) color else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MoodHistoryCard(entry: MoodEntry, onClick: () -> Unit) {
    val option = moodOption(entry.moodKey); val category = moodCategoryForOption(option)
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Surface(Modifier.size(44.dp), shape = CircleShape, color = option.colors.first().copy(alpha = 0.16f)) { Box(contentAlignment = Alignment.Center) { Text(option.emoji, style = MaterialTheme.typography.titleLarge) } }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.emoji + " " + category.label + " · " + option.label, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f)); Text(SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (entry.note.isNotBlank()) { Spacer(Modifier.height(6.dp)); Text(entry.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis) }
                Spacer(Modifier.height(6.dp)); Text("点击查看和编辑", style = MaterialTheme.typography.bodySmall, color = category.colors.first())
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MoodEntryDetailDialog(entry: MoodEntry, onDismiss: () -> Unit, onSave: (MoodEntry) -> Unit, onDelete: () -> Unit) {
    var categoryKey by remember(entry.id) { mutableStateOf(moodOption(entry.moodKey).categoryKey) }
    var moodKey by remember(entry.id) { mutableStateOf(entry.moodKey) }
    var note by remember(entry.id) { mutableStateOf(entry.note) }
    var confirmDelete by remember(entry.id) { mutableStateOf(false) }
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("删除这条记录？") }, text = { Text("删除后无法恢复。") }, confirmButton = { TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } })
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录详情") },
        text = {
            Column(Modifier.heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(friendlyDay(entry.createdAt) + " · " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(entry.createdAt)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { moodCategories.forEach { cat -> FilterPill(cat.emoji + " " + cat.label, categoryKey == cat.key, cat.colors.first()) { categoryKey = cat.key; moodKey = moodOptionsForCategory(cat.key).first().key } } }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { moodOptionsForCategory(categoryKey).forEach { op -> FilterPill(op.emoji + " " + op.label, moodKey == op.key, op.colors.first()) { moodKey = op.key } } }
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth(), label = { Text("内容") }, minLines = 5, maxLines = 10, shape = RoundedCornerShape(16.dp))
            }
        },
        confirmButton = { Button(onClick = { onSave(entry.copy(moodKey = moodKey, note = note)) }, shape = RoundedCornerShape(14.dp)) { Text("保存修改") } },
        dismissButton = { Row { TextButton(onClick = { confirmDelete = true }) { Text("删除", color = MaterialTheme.colorScheme.error) }; TextButton(onClick = onDismiss) { Text("取消") } } }
    )
}

@Composable
private fun EmptyHistoryCard() {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) { Text("当前筛选下没有记录。", Modifier.padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun withinTimeFilter(time: Long, filter: MoodTimeFilter): Boolean {
    if (filter == MoodTimeFilter.ALL) return true
    val now = Calendar.getInstance(); val target = Calendar.getInstance().apply { timeInMillis = time }
    return when (filter) {
        MoodTimeFilter.TODAY -> startOfDay(time) == startOfDay(System.currentTimeMillis())
        MoodTimeFilter.WEEK -> { val start = (now.clone() as Calendar).apply { firstDayOfWeek = Calendar.MONDAY; set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }; time >= start.timeInMillis }
        MoodTimeFilter.MONTH -> target.get(Calendar.YEAR) == now.get(Calendar.YEAR) && target.get(Calendar.MONTH) == now.get(Calendar.MONTH)
        MoodTimeFilter.ALL -> true
    }
}

private fun moodPrompt(key: String) = when (key) {
    "insight_clarity" -> "有些答案，想通的那一刻就轻了"; "insight_perspective" -> "换个角度，也许会看见新的出口"; "insight_acceptance" -> "先接纳自己，再慢慢成为更好的自己"; "insight_release" -> "放下不是遗忘，是把力气还给自己"
    "decision_action" -> "决定了，就从最小的一步开始"; "decision_goal" -> "目标清楚，脚下的路就会慢慢显现"; "decision_boundary" -> "守住边界，也是在照顾自己"; "decision_change" -> "改变不用轰烈，持续就很有力量"
    "growth_progress" -> "每一步都算数，你正在变好"; "growth_habit" -> "坚持到今天，已经很了不起"; "growth_focus" -> "找回专注，就是重新拿回主动权"; "growth_brave" -> "记住这一刻，你真的做到了"
    "happy" -> "今天真不错，保持这份开心"; "excited" -> "把这份能量，变成今天的动力"; "grateful" -> "记得把温柔和感谢说出来"; "hopeful" -> "心里有期待，日子就有方向"; "tired" -> "辛苦了，给自己一点休息时间"; "anxious" -> "慢慢来，一件一件处理"; "annoyed" -> "先深呼吸，把烦恼放轻一点"; "angry" -> "允许生气，但别让它伤到自己"; "low" -> "允许自己低落一会儿，明天再出发"; "sad" -> "难过也没关系，先陪陪自己"; "lonely" -> "一个人的时候，也值得被好好照顾"; "confused" -> "看不清路时，就先走好眼前一步"; "shy" -> "害羞也是可爱的一部分"; "neutral" -> "普通的一天，也值得认真记录"; else -> "今天感觉怎么样？"
}

private fun dayKey(time: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(time))
private fun friendlyDay(time: Long): String { val today = startOfDay(System.currentTimeMillis()); val day = startOfDay(time); return when (day) { today -> "今天 · " + SimpleDateFormat("M月d日", Locale.SIMPLIFIED_CHINESE).format(Date(time)); today - 86_400_000L -> "昨天 · " + SimpleDateFormat("M月d日", Locale.SIMPLIFIED_CHINESE).format(Date(time)); else -> SimpleDateFormat("M月d日 EEEE", Locale.SIMPLIFIED_CHINESE).format(Date(time)) } }
private fun isToday(time: Long) = startOfDay(time) == startOfDay(System.currentTimeMillis())
private fun startOfDay(time: Long) = Calendar.getInstance().apply { timeInMillis = time; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis