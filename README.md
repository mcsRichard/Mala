# Mala — 佛教修行记录 App

一款专为佛教修行者设计的功课记录 Android App，支持云端同步、计数器、统计分析与修行提醒。

---

## 功能

### 用户系统
- 邮箱 + 密码注册 / 登录
- Google 一键登录
- 个人主页（法名编辑、头像上传）
- 多设备数据自动同步（Firestore 双向同步）

### 功课管理
- **内置功课库**：心咒类、经文类、五加行、修法祈祷文类
- 自定义添加功课（名称、类型、目标）
- 目标类型：每日打卡 / 每日数量 / 终生累计 / 课程进度 / **限期完成**
- **限期完成目标**：设定总量与截止日期，自动计算每日配额（动态追赶）
- 拖拽排序功课顺序
- 修改目标数量、删除功课（带确认弹窗）

### 每日记录与计数
- 首页显示今日待完成 / 已完成功课分区
- 手动输入完成量
- **计数器页面**：
  - 大按钮点击计数，+7 / +21 / +108 快捷键
  - 震动反馈、音效开关、屏幕常亮
  - 横屏大按钮模式（全屏点击区域）
- 打卡完成自动归入已完成区，今日统计实时更新

### 统计
- 连续打卡天数（Streak）
- 本周每日完成情况（柱状图）
- 各功课累计数量（含限期完成进度条）

### 传承灌顶记录
- 记录所有已获传承与灌顶
- 字段：名称、上师、时间、地点、备注（全部必填）
- 支持添加、编辑、删除

### 共修小组
- 创建小组：设定共修功课与统一目标（当日完成 / 总目标，全组相同）
- **分享邀请**：生成 `mala://group/编号` 链接，可分享至微信 / WhatsApp 等；也可手动输入 6 位编号加入
- 受邀者点击链接自动打开 App，查看小组信息后确认加入
- 小组页查看每个小组今日打卡人数与个人累计进度
- 小组内打卡（当日完成 / 累计数量），组员列表实时显示各人状态
- 创建者为管理员：可修改功课目标、解散小组；组员可随时退出

### 数据导出
- 导出全部记录为 CSV 文件（带 UTF-8 BOM，Excel 可直接打开）
- 导出内容：功课记录（含目标、累计数）+ 传承灌顶
- 分 Section 结构，后续新记录类型自动纳入

### 提醒
- **无限自定义提醒时间**（不限早课 / 晚课，可添加任意数量）
- 通知内容智能生成：显示当天尚未完成的功课名称
  - 有未完成项：`修行提醒（还有 N 项）` + 展开显示完整列表
  - 全部完成：`今日功课已全部完成 ✓`
- 设备重启后自动恢复所有提醒

### 其他
- 中英文 / 繁简体自动切换
- 强制亮色模式（保持一致的暖色调风格）

---

## 技术栈

| 层次 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository |
| 本地存储 | Room (SQLite) v5 |
| 云端 | Firebase Auth · Firestore |
| 后台任务 | AlarmManager + BroadcastReceiver (`goAsync`) |
| 导航 | Navigation Compose |
| 异步 | Kotlin Coroutines + Flow |

- 最低支持：Android 8.0（API 26）
- 目标版本：Android 15（API 35）

---

## 项目结构

```
app/src/main/java/com/meritminder/app/
├── data/
│   ├── export/         # ExportManager（CSV 导出）
│   ├── local/          # Room DB v5、DAO、Entity
│   │   ├── dao/        # PracticeDao, GoalDao, DailyRecordDao, TransmissionDao, ReminderDao
│   │   └── entity/     # Practice, Goal, DailyRecord, Transmission, Reminder
│   └── remote/         # FirestoreSync
├── data/repository/    # PracticeRepository、AuthRepository
├── navigation/         # NavGraph
├── notification/       # ReminderScheduler, ReminderReceiver, NotificationHelper, BootReceiver
├── ui/
│   ├── auth/           # 登录 / 注册
│   ├── counter/        # 计数器
│   ├── home/           # 首页（今日功课）
│   ├── library/        # 功课库
│   ├── main/           # 主界面（底部导航）
│   ├── practice/       # 功课管理（添加、排序）
│   ├── profile/        # 个人主页
│   ├── settings/       # 提醒设置
│   ├── stats/          # 统计
│   ├── theme/          # 主题色
│   └── transmission/   # 传承灌顶
└── utils/              # LanguageManager 等工具
```

---

## 运行配置

1. 在 [Firebase Console](https://console.firebase.google.com) 创建项目，开启 Authentication 和 Firestore
2. 下载 `google-services.json` 放入 `app/` 目录（已加入 `.gitignore`，不随代码提交）
3. Firestore 规则参考：

```js
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /user_profiles/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    match /groups/{groupId} {
      allow read, create: if request.auth != null;
      allow update, delete: if request.auth != null
        && resource.data.creatorId == request.auth.uid;
      match /members/{memberId} {
        allow read: if request.auth != null;
        allow write: if request.auth != null && (
          memberId == request.auth.uid ||
          get(/databases/$(database)/documents/groups/$(groupId)).data.creatorId == request.auth.uid
        );
      }
      match /checkins/{checkinId} {
        allow read: if request.auth != null;
        allow write: if request.auth != null && (
          checkinId.matches(request.auth.uid + '_.*') ||
          get(/databases/$(database)/documents/groups/$(groupId)).data.creatorId == request.auth.uid
        );
      }
    }
  }
}
```

4. 用 Android Studio 打开项目，Sync Gradle，运行即可

---

## 开发工具

- Android Studio（模拟器、调试、APK 打包）
- Firebase Console（数据库、用户管理）
- Claude Code（AI 辅助开发）
