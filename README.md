# 汐眠 TideSleep

连续粉红噪声助眠；支持立即、倒计时与定时三种启动方式；可选入睡触发稀疏脉冲模式。

> 无 EEG 时为开放环（入睡触发），不等于论文级相位闭环。本项目不提供医疗诊断或治疗建议。

**当前版本：0.4.0-mvp**

---

## 产品简介

**汐眠（TideSleep）** 是一款睡眠音频助手：

- **立即**：首页选择「立即」后点击 **开始播放**，即刻开始连续 1/f 粉红噪声
- **倒计时**：选择时长（5–60 分钟）后 **开始倒计时**，归零后自动开始连续播放
- **定时**：选择时钟时间后 **设定定时**，到点自动开始连续播放（优先 AlarmManager 精确闹钟，不可用时回退 WorkManager）
- **高级路径**：穿戴确认入睡后触发稀疏 50 ms 脉冲（米家自动化 / 小米穿戴 SDK / 设备页调试选项）
- 音量受 **安全设置** 上限约束

科学锚点：在慢波峰值时机播放短脉冲可强化慢波与脑脊液（CSF）潮汐（参见 [调研与实施方案](docs/调研与实施方案.md)）。

### v0.4.0 变更

| 首页体验 | 说明 |
|---|---|
| 正式化文案与排版 | 去除口语化比喻，采用「粉红噪声 · 连续播放」等产品表述 |
| 三种启动模式 | 立即 / 倒计时 / 定时，偏好持久化至 DataStore |
| 待命态进度环 | 倒计时或定时待命时，主按钮外圈显示剩余进度 |
| 设备状态卡片 | 手表 / 音箱 / 手机，简短正式状态标签 |

### v0.3.0 变更

| 默认体验 | 高级模式（保留） |
|---|---|
| 首页点击即播连续粉红噪声 | 入睡触发稀疏 50 ms 脉冲 |
| 大红「停止」按钮 | 设备页 → 展开调试选项 |
| 无延迟、无模拟按钮作为主路径 | 米家 / 穿戴 SDK 入睡检测 |

---

## 睡眠检测双通路

| 通路 | 说明 | 是否需要 AAR |
|------|------|-------------|
| **米家自动化** | 深链 `tidesleep://sleep?state=asleep\|awake` 或广播 `ACTION_SLEEP_STATE` | 否 |
| **小米穿戴 SDK** | `query/subscribe` `ITEM_SLEEP`，DEVICE_MANAGER 权限 | 是（开放平台申请） |
| **演示模式** | 设备页调试选项中手动模拟 | 否 |

详见 [小米穿戴与米家接入](docs/小米穿戴与米家接入.md)。

---

## 安全预设

| 预设 | 入睡后延迟 | 单晚上限 | 音量 | 用途 |
|------|-----------|---------|------|------|
| **演示**（首次安装默认） | 30 秒 | 10 分钟 | 20% | 快速验证 |
| **科学默认** | 15 分钟 | 90 分钟 | 25% | 对齐论文开放环近似 |
| **自定义** | 可调 | 可调 | 可调 | 微调后自动切换 |

设置路径：**我的 → 安全设置**，可「试听一发脉冲」校准音量。连续播放与脉冲模式共用音量上限。

---

## 架构概览

```
首页 CTA ──► PlaybackStartManager（立即 / 倒计时 / 定时）
                    │
                    ▼
         ContinuousPinkNoisePlayer（连续 1/f 粉红噪声）
                    │
穿戴入睡/出睡 ──► SessionEngine（稀疏脉冲，高级模式）
         ▲              │
   米家自动化深链       ▼
   或 Wear SDK     手机扬声器
```

| 模块 | 路径 | 说明 |
|------|------|------|
| 启动调度 | `playback/PlaybackStartManager.kt` | 三种模式状态机 + DataStore |
| 精确闹钟 | `playback/PinkNoiseScheduler.kt` | AlarmManager / WorkManager 回退 |
| 连续播放 | `audio/ContinuousPinkNoisePlayer.kt` | STREAM AudioTrack 循环写入 |
| 粉红噪声 | `audio/PinkNoiseGenerator.kt` | 1/f 合成 |
| 会话引擎 | `session/SessionEngine.kt` | 入睡触发稀疏脉冲（高级） |
| 持久化 | `data/TideSleepRepository.kt` | DataStore：配置、来源、免责、夜晚记录、启动偏好 |
| UI | `ui/screens/` | Compose 四大 Tab + 会话页 + Onboarding |

### 定时与精确闹钟

Android 12+ 在 `AlarmManager.canScheduleExactAlarms()` 为真时使用精确闹钟；否则通过 WorkManager 一次性任务在目标时间触发播放。Manifest 声明 `SCHEDULE_EXACT_ALARM`，仅在系统允许时实际请求精确调度权限。

---

## 仓库结构

```
.
├── README.md
├── docs/
│   ├── 小米穿戴与米家接入.md
│   ├── 调研与实施方案.md
│   └── prototypes/
└── android/
    ├── app/libs/          # 官方 wearable AAR（自行下载）
    └── wear-stubs/        # compileOnly SDK 桩
```

---

## 构建与运行

### 环境要求

- **Android Studio** Ladybug (2024.2) 或更高
- **JDK 17+**
- **Android SDK 35**
- **minSdk 26**

### 本地构建（无需小米 AAR）

```bash
cd android
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

### 默认体验（连续粉红噪声）

1. **首次启动** → 勾选非医疗免责 → 「开始使用」
2. **今晚** → 选择 **立即 / 倒计时 / 定时** → 点击主按钮
3. 播放中点击 **停止** 结束

### 高级：入睡触发稀疏脉冲（演示模式）

1. **设备** → 「演示模式」→ 展开 **调试选项**
2. 「模拟入睡」→ 等待延迟 → 稀疏脉冲播放
3. 「模拟醒来」→ **记录** Tab 查看历史

### 米家自动化流程（真机）

1. **设备** → 选择 **米家自动化**
2. 设备页调试选项启动入睡触发会话
3. 米家配置：睡着 → `tidesleep://sleep?state=asleep`；醒来 → `tidesleep://sleep?state=awake`
4. 详见 [小米穿戴与米家接入](docs/小米穿戴与米家接入.md)

### 小米穿戴 SDK

1. [dev.mi.com](https://dev.mi.com/) 申请，包名 `com.tidesleep.app`
2. AAR 放入 `android/app/libs/`
3. 重新编译，设备页选择 **小米穿戴 SDK**

---

## 许可证

待定（MVP 脚手架阶段）。
