# 任务提醒 App 2.0

一个自用的 Android 原生任务提醒应用，使用 Kotlin、Jetpack Compose、Room 和前台服务实现。

## 本次版本重点

1. 通知栏常驻改为 Android 前台服务实现，不依赖 App 界面一直存活。
2. 即使暂时没有待办，也会显示“任务提醒正在运行”的常驻通知。
3. 常驻通知每分钟刷新最近任务和倒计时。
4. 通知通道升级为 `task_reminder_ongoing_v2`，避免旧版本最低重要性通道影响显示。
5. 手机重启或应用更新后自动恢复闹钟和常驻通知服务。
6. 重新设计首页、新建/编辑页、权限引导页和深浅色主题。
7. 加入 iQOO / OriginOS 自启动、电池优化和后台运行设置说明。

## 包内目录

```text
TaskReminder/
├── .github/workflows/build.yml
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── README.md
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/example/taskreminder/
        │   ├── MainActivity.kt
        │   ├── alarm/
        │   │   ├── AlarmScheduler.kt
        │   │   ├── AlarmReceiver.kt
        │   │   ├── NotificationActionReceiver.kt
        │   │   ├── NotificationHelper.kt
        │   │   ├── BootReceiver.kt
        │   │   └── OngoingReminderService.kt
        │   ├── data/
        │   ├── ui/
        │   │   ├── PermissionScreen.kt
        │   │   ├── TaskListScreen.kt
        │   │   ├── TaskEditScreen.kt
        │   │   └── theme/Theme.kt
        │   ├── util/
        │   │   ├── BackupUtil.kt
        │   │   ├── NaturalLanguageParser.kt
        │   │   └── OngoingNotifier.kt
        │   └── vm/TaskViewModel.kt
        └── res/
```

## 功能

- 自然语言快速添加任务
- 今天、打卡、计划三个任务分区
- 每天、工作日、每周、每月、间隔重复
- 提前提醒、到点提醒、稍后 5 分钟
- 打卡连续天数记录
- 已完成任务显示与清理
- 通知栏最近任务常驻和倒计时刷新
- JSON 备份导出、导入
- 系统深浅色自动跟随

## 编译 APK

### GitHub Actions

1. 将包内所有文件上传到 GitHub 仓库根目录。
2. 推送到 `main` 分支会自动执行构建。
3. 也可以进入 `Actions -> Build APK -> Run workflow` 手动执行。
4. 构建完成后，在对应任务页面的 `Artifacts` 中下载 `TaskReminder-APK`。

### Android Studio

直接导入项目根目录，使用 JDK 17 和 Android SDK 34 进行构建。

## 首次安装

打开 App 后，按引导依次开启：

1. 通知权限
2. 精确闹钟
3. 电池不优化
4. 自启动与后台运行

## iQOO 12 / OriginOS 设置

为了保证通知栏常驻通知不被系统清理，建议完成以下项目：

1. 设置 -> 应用与权限 -> 应用管理 -> 任务提醒 -> 权限 -> 自启动：允许。
2. 设置 -> 电池 -> 后台耗电管理 -> 任务提醒：允许后台高耗电、允许后台运行。
3. 在最近任务界面给“任务提醒”加锁，避免一键清理时关闭。
4. 通知设置中确认“任务常驻提醒”通道没有被关闭。
5. 如果系统询问是否允许后台运行，选择“始终允许”。

说明：Android 不允许应用绕过用户设置强制常驻。本应用使用前台服务维持通知，系统强制停止应用后仍需要由用户重新打开，或者等待系统按 `START_STICKY` 恢复服务。

## 使用说明

- 顶部快捷输入支持：`今天十点二十洗碗`、`明天下午三点开会`、`每天八点吃药`。
- 点击任务左侧圆圈完成任务。
- 打卡任务完成后会保留连续天数。
- 右上角眼睛图标用于显示或隐藏已完成任务。
- 右上角菜单包含导出、导入、清理已完成和后台权限设置。

## 备份与恢复

右上角菜单 -> `导出备份` / `导入备份`。
