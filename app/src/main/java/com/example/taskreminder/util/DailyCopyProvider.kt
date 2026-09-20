package com.example.taskreminder.util

import android.content.Context
import android.icu.util.ChineseCalendar
import java.util.Calendar
import java.util.Date
import java.util.Random

/**
 * 常驻通知每日文案。
 *
 * 优先判断农历节日、公历节日和二十四节气；普通日期从 assets/daily_quotes.txt
 * 中按日期顺序抽取。每个完整轮次会使用新的排列，因此同一轮内不会重复。
 */
object DailyCopyProvider {
    data class DailyCopy(
        val text: String,
        val holidayName: String? = null
    )

    private data class HolidayTheme(
        val name: String,
        val texts: List<String>
    )

    @Volatile
    private var cachedQuotes: List<String>? = null

    fun forDate(context: Context, timeMillis: Long = System.currentTimeMillis()): DailyCopy {
        val holiday = findHoliday(timeMillis)
        val options = holiday?.texts?.takeIf { it.isNotEmpty() } ?: loadQuotes(context)
        val safeOptions = options.ifEmpty { listOf("愿你今天平安喜乐。") }
        val index = pickIndex(timeMillis, safeOptions.size, holiday != null)
        return DailyCopy(
            text = safeOptions[index],
            holidayName = holiday?.name
        )
    }

    private fun pickIndex(timeMillis: Long, size: Int, holiday: Boolean): Int {
        if (size <= 1) return 0
        val dayNumber = Math.floorDiv(timeMillis, 86_400_000L)
        if (holiday) {
            return Math.floorMod(dayNumber, size.toLong()).toInt()
        }
        val cycle = Math.floorDiv(dayNumber, size.toLong()).toInt()
        val offset = Math.floorMod(dayNumber, size.toLong()).toInt()
        val order = MutableList(size) { it }
        order.shuffle(Random(cycle * 7919L + 17L))
        return order[offset]
    }

    private fun loadQuotes(context: Context): List<String> {
        cachedQuotes?.let { return it }
        synchronized(this) {
            cachedQuotes?.let { return it }
            val quotes = runCatching {
                context.assets.open("daily_quotes.txt")
                    .bufferedReader(Charsets.UTF_8)
                    .useLines { lines ->
                        lines.map { it.trim() }
                            .filter { it.isNotEmpty() && !it.startsWith("#") }
                            .distinct()
                            .toList()
                    }
            }.getOrDefault(emptyList())
            cachedQuotes = quotes
            return quotes
        }
    }

    private fun findHoliday(timeMillis: Long): HolidayTheme? {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val lunar = lunarDate(timeMillis)
        val tomorrow = lunarDate(timeMillis + 86_400_000L)

        // 农历节日优先，除夕单独判断“明天是正月初一”。
        if (lunar == (1 to 1)) return holiday("春节")
        if (lunar == (1 to 15)) return holiday("元宵节")
        if (lunar == (2 to 2)) return holiday("龙抬头")
        if (lunar == (5 to 5)) return holiday("端午节")
        if (lunar == (7 to 7)) return holiday("七夕")
        if (lunar == (7 to 15)) return holiday("中元节")
        if (lunar == (8 to 15)) return holiday("中秋节")
        if (lunar == (9 to 9)) return holiday("重阳节")
        if (lunar == (12 to 8)) return holiday("腊八节")
        if (lunar == (12 to 23)) return holiday("小年")
        if (tomorrow == (1 to 1)) return holiday("除夕")

        solarHoliday(month, day)?.let { return holiday(it) }
        dynamicHoliday(cal)?.let { return holiday(it) }
        solarTerm(month, day)?.let { return holiday(it) }
        return null
    }

    private fun solarHoliday(month: Int, day: Int): String? = when (month to day) {
        1 to 1 -> "元旦"
        2 to 14 -> "情人节"
        3 to 8 -> "妇女节"
        3 to 12 -> "植树节"
        4 to 22 -> "世界地球日"
        5 to 1 -> "劳动节"
        5 to 4 -> "青年节"
        6 to 1 -> "儿童节"
        6 to 5 -> "世界环境日"
        7 to 1 -> "建党节"
        8 to 1 -> "建军节"
        9 to 10 -> "教师节"
        10 to 1 -> "国庆节"
        12 to 24 -> "平安夜"
        12 to 25 -> "圣诞节"
        else -> null
    }

    private fun dynamicHoliday(cal: Calendar): String? {
        val month = cal.get(Calendar.MONTH) + 1
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val weekOfMonth = (dayOfMonth - 1) / 7 + 1
        return when {
            month == 5 && dayOfWeek == Calendar.SUNDAY && weekOfMonth == 2 -> "母亲节"
            month == 6 && dayOfWeek == Calendar.SUNDAY && weekOfMonth == 3 -> "父亲节"
            month == 11 && dayOfWeek == Calendar.THURSDAY && weekOfMonth == 4 -> "感恩节"
            else -> null
        }
    }

    private fun solarTerm(month: Int, day: Int): String? = mapOf(
        1 to 5 to "小寒", 1 to 20 to "大寒",
        2 to 4 to "立春", 2 to 19 to "雨水",
        3 to 5 to "惊蛰", 3 to 20 to "春分",
        4 to 4 to "清明", 4 to 5 to "清明", 4 to 20 to "谷雨",
        5 to 5 to "立夏", 5 to 21 to "小满",
        6 to 5 to "芒种", 6 to 21 to "夏至",
        7 to 7 to "小暑", 7 to 22 to "大暑",
        8 to 7 to "立秋", 8 to 23 to "处暑",
        9 to 7 to "白露", 9 to 23 to "秋分",
        10 to 8 to "寒露", 10 to 23 to "霜降",
        11 to 7 to "立冬", 11 to 22 to "小雪",
        12 to 7 to "大雪", 12 to 21 to "冬至", 12 to 22 to "冬至"
    )[month to day]

    private fun lunarDate(timeMillis: Long): Pair<Int, Int> {
        val lunar = ChineseCalendar.getInstance().apply { time = Date(timeMillis) }
        return (lunar.get(Calendar.MONTH) + 1) to lunar.get(Calendar.DAY_OF_MONTH)
    }

    private fun holiday(name: String): HolidayTheme = HolidayTheme(
        name = name,
        texts = holidayTexts[name] ?: fallbackHolidayText(name)
    )

    private fun fallbackHolidayText(name: String): List<String> {
        val solarTerms = setOf(
            "小寒", "大寒", "立春", "雨水", "惊蛰", "春分", "清明", "谷雨",
            "立夏", "小满", "芒种", "夏至", "小暑", "大暑", "立秋", "处暑",
            "白露", "秋分", "寒露", "霜降", "立冬", "小雪", "大雪", "冬至"
        )
        return if (name in solarTerms) {
            listOf(
                "$name至，四时流转。愿你顺应节气，安顿身心，从容生活。",
                "今日$name，风物有序。愿你在季节更迭里，收获属于自己的安宁。"
            )
        } else {
            listOf("愿你在这一天，收获独属于当下的温柔。")
        }
    }

    private val holidayTexts = mapOf(
        "元旦" to listOf("一元复始，万象更新。愿新的一年，所求皆如愿，所行皆坦途。", "新的一年，愿你心中有光，脚下有路，所遇皆美好。"),
        "春节" to listOf("爆竹声中辞旧岁，春风送暖入屠苏。愿新年胜旧年，岁岁常欢愉。", "新春安康，万事顺遂。愿家人闲坐，灯火可亲。", "千门万户曈曈日，总把新桃换旧符。愿新岁平安喜乐。"),
        "元宵节" to listOf("东风夜放花千树，更吹落、星如雨。愿灯火可亲，有人相伴，有梦可期。", "月色婵娟，灯火辉煌。愿人间团圆，岁岁长安。"),
        "龙抬头" to listOf("二月二，春意升起。愿你好运抬头，万事都有好兆头。", "春龙抬首，鸿运当头。愿所有心愿，都有回响。"),
        "清明节" to listOf("梨花风起正清明。愿故人安好，愿生者珍惜眼前。", "清明时节，细雨轻落。愿思念有所归处，生活安然如初。"),
        "劳动节" to listOf("劳动最光荣，也愿努力生活的人，都能收获温柔的回馈。", "愿每一份认真付出，都被岁月温柔记住。"),
        "母亲节" to listOf("岁月温柔，愿你也被温柔以待。记得对妈妈说一声爱你。", "妈妈的爱，是岁月里不熄的灯火。愿她平安喜乐，岁岁无忧。"),
        "儿童节" to listOf("愿你历尽千帆，归来仍是少年。", "童心未泯，所遇皆甜。愿我们都能保留一份纯粹的快乐。"),
        "父亲节" to listOf("父爱如山，沉默却坚定。愿父亲平安健康，岁岁安好。", "感谢那个默默为你撑起一片天空的人。"),
        "端午节" to listOf("彩线轻缠红玉臂，小符斜挂绿云鬟。愿端午安康，万事顺遂。", "粽叶飘香，龙舟竞渡。愿所求皆如愿，所行化坦途。"),
        "七夕" to listOf("金风玉露一相逢，便胜却人间无数。愿真心不被辜负，爱意岁岁年年。", "七夕星河长明，愿你被人偏爱，亦懂得珍惜自己。"),
        "中元节" to listOf("月色如水，思念无声。愿故人安息，愿生者珍重。", "记得来处，也珍惜当下；愿所有牵挂，都被时光温柔安放。"),
        "中秋节" to listOf("但愿人长久，千里共婵娟。愿花好月圆，人长久，共清欢。", "海上生明月，天涯共此时。愿月色温柔，团圆常在。", "今夜月明人尽望，不知秋思落谁家。愿所念之人皆平安。"),
        "重阳节" to listOf("遥知兄弟登高处，遍插茱萸少一人。愿登高望远，所念皆安。", "九九重阳，天高云淡。愿岁月从容，长辈安康。"),
        "国庆节" to listOf("山河锦绣，国泰民安。愿盛世如愿，愿我们都在热爱的生活里闪闪发光。", "今天，为祖国送上祝福，也为自己许一个新的开始。"),
        "教师节" to listOf("春风化雨，润物无声。愿每一份教诲，都开出芬芳的花。", "感谢点亮前路的人。愿所有老师节日快乐，桃李芬芳。"),
        "平安夜" to listOf("愿平安不止今夜，愿欢喜常伴余生。", "灯火温柔，夜色安宁。愿你所盼皆如愿，所行皆坦途。"),
        "圣诞节" to listOf("圣诞快乐，愿你被世界温柔相待。", "愿冬日有暖阳，心里有期待，生活有惊喜。"),
        "感恩节" to listOf("感谢岁月赠予的每一次相遇，也感谢一直没有放弃的自己。", "常怀感恩之心，所遇皆是温柔。"),
        "世界地球日" to listOf("万物有灵，山水有情。愿我们温柔地爱护这个共同的家。", "守护地球，也是守护每一个明天。"),
        "世界环境日" to listOf("青山绿水，是大自然写给人类最长情的诗。", "愿我们脚下的土地，永远生机勃勃。"),
        "世界读书日" to listOf("脚步丈量不到的地方，文字可以。", "读书，是在别人的故事里遇见更辽阔的自己。"),
        "情人节" to listOf("愿你有爱人的能力，也有被爱的运气。", "爱意不必张扬，长久便是答案。"),
        "妇女节" to listOf("愿你不被定义，勇敢做自己；愿你温柔坚定，自由闪光。", "你可以是玫瑰，也可以是松柏；愿你成为任何想成为的样子。"),
        "植树节" to listOf("种下一棵树，最好的时间是十年前，其次是现在。", "愿所有微小的坚持，都在未来长成一片绿荫。"),
        "青年节" to listOf("愿中国青年都摆脱冷气，只是向上走。", "青春不是一段时光，而是一种勇敢向前的心境。"),
        "建党节" to listOf("不忘初心，方得始终。愿我们在各自岗位上，认真生活，坚定前行。", "心中有信仰，脚下有力量。"),
        "建军节" to listOf("山河无恙，因为有人为你负重前行。致敬最可爱的人。", "愿每一份守护，都被岁月温柔以待。"),
        "元宵" to listOf("灯火可亲，人间团圆。愿新岁平安喜乐。"),
        "小年" to listOf("小年纳福，烟火可亲。愿新岁平安，所愿皆成。"),
        "腊八节" to listOf("腊八粥香，年味渐浓。愿你喝下温暖，迎来好运。"),
        "除夕" to listOf("旧岁至此而除，明朝又是新岁。愿家人闲坐，灯火可亲。", "岁暮天寒，愿你与温暖撞个满怀，新年万事胜意。")
    )
}


