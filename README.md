# 任务提醒 TaskReminder

一个轻量、自用的 Android 原生任务提醒 App。到点弹通知，支持重复规则、习惯打卡、自然语言快速添加，通知栏常驻显示今日待办。

<div align="left">

[![Build APK](https://github.com/namecwl/TaskReminder/actions/workflows/build.yml/badge.svg)](https://github.com/namecwl/TaskReminder/actions)
[![Release](https://img.shields.io/github/v/release/namecwl/TaskReminder)](https://github.com/namecwl/TaskReminder/releases)
</div>

## 功能

- **到点提醒** — AlarmManager 精确闹钟，到点弹通知
- **重复规则** — 每天 / 工作日 / 每周 / 每月 / 间隔天数
- **提前提醒** — 可设置提前 N 分钟提醒
- **习惯打卡** — 完成任务记录连续天数（streak）
- **自然语言输入** — 输入"明天下午三点开会"自动解析时间
- **通知栏常驻** — 前台服务实时显示最近任务和倒计时
- **快捷操作** — 通知栏直接完成 / 稍后 5 分钟 / 打开 App
- **备份恢复** — JSON 导出导入，换手机不丢数据
- **深色模式** — 自动跟随系统
- **开机自启** — 重启后自动恢复闹钟和常驻通知

## 下载安装

前往 [Releases](https://github.com/namecwl/TaskReminder/releases) 下载最新 pp-release.apk，传到手机安装即可。

## 首次设置

安装后按引导开启以下权限：

1. **通知权限** — 允许发送通知
2. **精确闹钟** — 允许设置精确闹钟
3. **电池不优化** — 允许后台运行
4. **自启动** — 允许应用自启动（iQOO / OriginOS：设置 → 电池 → 后台高耗电 → 允许）

## 使用说明

- 顶部输入框输入 今天十点二十洗碗、每天八点吃药、工作日九点打卡
- 点击任务左侧圆圈完成任务
- 打卡任务自动计算连续天数
- 右上角眼睛图标：显示/隐藏已完成
- 右上角菜单：导出备份、导入备份、清理已完成、后台设置

## 技术栈

- Kotlin + Jetpack Compose
- Room Database
- AlarmManager + BroadcastReceiver
- Foreground Service（常驻通知）
- GitHub Actions 自动构建 APK

## 从源码构建

推送到 main 分支会自动触发 GitHub Actions 构建。也可以本地用 Android Studio（JDK 17, SDK 34）直接构建。

## License

仅供个人使用。