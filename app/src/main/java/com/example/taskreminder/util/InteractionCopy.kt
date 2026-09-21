package com.example.taskreminder.util

import java.time.Instant
import java.time.ZoneId

/**
 * 界面交互文案库。
 *
 * 文案按“日期 + 场景”稳定选择：同一天不会频繁跳变，跨天会自然轮换。
 * 页面结构和组件分布保持不变，只通过这里丰富提示语和反馈语。
 */
object InteractionCopy {
    private val quickAddHints = listOf(
        "一句话记一下，例如「晚上8点拿快递」",
        "直接说时间，例如「明天下午三点开会」",
        "想养成习惯？试试「每天八点吃药」",
        "有截止时间就说出来，例如「周五下午交报告」",
        "先记下来，再慢慢整理，例如「9月30日续费会员」",
        "可以带上地点，例如「明天十点去银行」",
        "想重复的任务试试「工作日九点打卡」",
        "临时想到什么，就先写一句完整的话",
        "支持今天、明天、后天和具体日期",
        "例如「每隔三天浇花」「每周一复盘」",
        "把动作写清楚，例如「晚上九点收拾书桌」",
        "越具体，提醒就越省心，例如「提前30分钟提醒开会」"
    )

    private val todayEmptyStates = listOf(
        "今天还没有安排" to "留白也很好，有新的念头就记在下面",
        "今天暂时空着" to "可以慢慢来，也可以马上安排一件小事",
        "今天的清单很轻" to "输入一句话，让提醒替你记住它",
        "还没有今天的任务" to "先写最重要的一件事，今天就会更有方向",
        "今天没有待办" to "把想做的事写下来，心里会更踏实",
        "今天还在等一个计划" to "从一句自然语言开始，不用填复杂表单",
        "今天很从容" to "需要提醒时，直接在上面说一句话就好",
        "今天的节奏由你决定" to "可以休息，也可以安排一件期待的事"
    )

    private val habitEmptyStates = listOf(
        "还没有习惯打卡" to "输入「每天八点吃药」即可创建",
        "习惯花园还是空的" to "从一个轻松做到的小习惯开始",
        "还没有正在坚持的事" to "试试「工作日九点读书」",
        "第一颗习惯种子还没种下" to "每天重复一次，系统会自动累计",
        "这里还没有打卡记录" to "例如「每天晚上十点拉伸」",
        "暂时没有习惯" to "把想坚持的事写成一句话"
    )

    private val planEmptyStates = listOf(
        "未来暂时没有计划" to "输入「明天下午三点开会」快速安排",
        "后面的日程还很空" to "提前记下来，到时候就不会慌",
        "还没有未来计划" to "例如「后天上午十点体检」",
        "未来几天很从容" to "需要时，用一句话安排到具体日期",
        "计划栏正在等待内容" to "试试「每周一上午做周计划」",
        "暂时没有后续安排" to "把重要日期说出来，我来帮你记住",
        "未来清单还是空的" to "明天的约定，也可以现在就记下",
        "暂无未来事项" to "先安排一件最不想忘记的事"
    )

    fun topSubtitle(now: Long = System.currentTimeMillis()): String =
        "今天 · ${AppTimeText.weekdayDate(now)}"

    fun greeting(now: Long = System.currentTimeMillis()): String {
        val hour = Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).hour
        val options = when {
            hour < 6 -> listOf("夜深了，慢一点也没关系", "新的一天正在来的路上", "安静地把心事安排好")
            hour < 11 -> listOf("早上好，先做最重要的一件事", "新的一天，慢慢进入状态", "给今天一个清晰的开始")
            hour < 14 -> listOf("中午好，记得给自己留一点空隙", "忙到一半，也别忘了休息", "稳住节奏，事情会一件件完成")
            hour < 18 -> listOf("下午好，继续按自己的节奏来", "把注意力放回眼前这一件事", "今天还长，不必着急")
            hour < 22 -> listOf("晚上好，收一收今天的尾声", "把没完成的事安心留到明天", "辛苦了，慢慢整理今天")
            else -> listOf("今天辛苦了，早点休息", "把明天交给清单，把今晚留给自己", "夜色渐深，愿你安心收尾")
        }
        return options[stableIndex(now, options.size, 11)]
    }

    fun quickAddHint(now: Long = System.currentTimeMillis()): String =
        quickAddHints[stableIndex(now, quickAddHints.size, 23)]

    fun emptyToday(now: Long = System.currentTimeMillis()): Pair<String, String> =
        todayEmptyStates[stableIndex(now, todayEmptyStates.size, 37)]

    fun emptyHabit(now: Long = System.currentTimeMillis()): Pair<String, String> =
        habitEmptyStates[stableIndex(now, habitEmptyStates.size, 53)]

    fun emptyPlan(now: Long = System.currentTimeMillis()): Pair<String, String> =
        planEmptyStates[stableIndex(now, planEmptyStates.size, 71)]

    fun moodCardPrompt(now: Long = System.currentTimeMillis()): String = listOf(
        "记录此刻的感觉",
        "今天的心情，也值得留一笔",
        "此刻的你，是什么感受？",
        "把心情放在这里，慢慢说给自己听",
        "给今天留下一个真实的注脚",
        "无论晴雨，都先看见自己的感受"
    )[stableIndex(now, 6, 127)]

    fun moodSavedFeedback(now: Long = System.currentTimeMillis()): String = listOf(
        "已记录，可以继续添加",
        "这一刻已经收好",
        "心情已保存，随时可以再记",
        "写下来之后，心里会轻一点",
        "已留下今天的这段感受"
    )[stableIndex(now, 5, 131)]

    fun permissionIntro(now: Long = System.currentTimeMillis()): String = listOf(
        "开启通知、常驻提醒和后台权限，让重要的事不因系统限制而迟到。",
        "把提醒交给系统稳定运行，你只需要专心过好今天。",
        "完成下面的设置后，任务会更准时，常驻通知也不容易被打断。",
        "花一分钟完成权限设置，换取之后每一次可靠提醒。",
        "通知、闹钟和后台权限越完整，提醒体验就越稳定。"
    )[stableIndex(now, 5, 101)]

    fun moodComposerIntro(now: Long = System.currentTimeMillis()): String = listOf(
        "同一天可以记录多次，每次都会按时间保存。",
        "不用写得完整，留下此刻最真实的一句话就好。",
        "开心或低落都可以记下来，情绪值得被认真看见。",
        "这是一条只属于今天的心情注脚，想到什么就写什么。",
        "记录不是为了评判，而是为了更了解自己。"
    )[stableIndex(now, 5, 113)]

    fun exportSuccess(): String = listOf(
        "完整备份已导出，数据已经安全收好",
        "备份导出完成，换机时可以直接导入",
        "数据已打包完成"
    )[stableIndex(System.currentTimeMillis(), 3, 89)]

    fun importSuccess(tasks: Int, moods: Int, habits: Int): String =
        "导入完成：任务 $tasks、心情 $moods、打卡 $habits"

    fun saveSuccess(isNew: Boolean): String =
        if (isNew) "任务已记下，到时间我会提醒你" else "修改已保存"

    private fun stableIndex(timeMillis: Long, size: Int, salt: Int): Int {
        if (size <= 1) return 0
        val day = Math.floorDiv(timeMillis, 86_400_000L)
        return Math.floorMod(day + salt.toLong(), size.toLong()).toInt()
    }
}



