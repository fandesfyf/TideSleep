# 汐眠 TideSleep

入睡后粉红噪声脉冲助眠；米家手表检测入睡后播放；基于慢波 / CSF 研究的开放环 MVP。

> 无 EEG 时为开放环（入睡触发），不等于论文级相位闭环。本项目不提供医疗诊断或治疗建议。

**当前版本：0.2.0-mvp**

---

## 产品简介

**汐眠（TideSleep）** 是一款「确认入睡后再播放」的睡眠音频助手：

- 由小米手表/手环判断入睡与出睡（**米家自动化** 或 **小米穿戴 SDK**）
- 在安全音量下以 **约 50 ms 粉红噪声短脉冲**（非整夜连续噪声）通过手机扬声器播放
- 远期可接入 EEG 头环，逼近 MIT 闭环方案

科学锚点：在慢波峰值时机播放短脉冲可强化慢波与脑脊液（CSF）潮汐（参见 [调研与实施方案](docs/调研与实施方案.md)）。

### 开放环诚实声明

| 本 MVP 能做到 | 本 MVP 做不到 |
|---|---|
| 穿戴确认「已入睡 / 已醒来」后触发播音 | EEG 慢波相位检测与闭环锁定 |
| 稀疏 50 ms 粉红噪声脉冲 | 整夜连续背景噪声 |
| 出睡即停、单晚上限、入睡延迟 | 医疗疗效承诺 |

---

## 睡眠检测双通路（v0.2.0）

| 通路 | 说明 | 是否需要 AAR |
|------|------|-------------|
| **米家自动化** | 深链 `tidesleep://sleep?state=asleep\|awake` 或广播 `ACTION_SLEEP_STATE` | 否 |
| **小米穿戴 SDK** | `query/subscribe` `ITEM_SLEEP`，DEVICE_MANAGER 权限 | 是（开放平台申请） |
| **演示模式** | Fake 手动模拟 | 否 |

详见 [小米穿戴与米家接入](docs/小米穿戴与米家接入.md)。

---

## 安全预设

| 预设 | 入睡后延迟 | 单晚上限 | 音量 | 用途 |
|------|-----------|---------|------|------|
| **演示**（首次安装默认） | 30 秒 | 10 分钟 | 20% | Fake 流程 ≤1 分钟 |
| **科学默认** | 15 分钟 | 90 分钟 | 25% | 对齐论文开放环近似 |
| **自定义** | 可调 | 可调 | 可调 | 微调后自动切换 |

设置路径：**我的 → 安全设置**，可「试听一发脉冲」校准音量。

---

## 架构概览

```
小米手表/手环 ──入睡/出睡──► 汐眠 App（会话引擎 + 音频引擎）
         ▲                        │
         │                        ▼
   米家自动化深链            手机扬声器（稀疏脉冲）
   或 Wear SDK subscribe
```

**会话状态机**：`Idle → Arming → WaitingSleep → WaitingDelay → Stimulating → Stopped`

| 模块 | 路径 | 说明 |
|------|------|------|
| 会话引擎 | `session/SessionEngine.kt` | 延迟开播、超时、出睡即停、历史归档 |
| 持久化 | `data/TideSleepRepository.kt` | DataStore：配置、来源、免责、夜晚记录 |
| 音频 | `audio/PinkNoisePulsePlayer.kt` | 50 ms 1/f 粉红噪声合成 |
| 米家桥接 | `wearable/MiJiaSleepBridgeMonitor.kt` | 深链/广播 → 睡眠状态 |
| 小米 SDK | `wearable/XiaomiWearSleepMonitor.kt` | 可选 AAR，`wear-stubs` 编译桩 |
| UI | `ui/screens/` | Compose 四大 Tab + 会话页 + Onboarding |

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

### 演示流程（Fake 模式，≤1 分钟）

1. **首次启动** → 勾选非医疗免责 → 「开始使用」
2. **今晚** → 「开启今晚」
3. **设备** → 「演示模式」→ 「模拟入睡」
4. 等待 **30 秒** → 进入脉冲播放
5. 「模拟醒来」→ **记录** Tab 查看历史

### 米家自动化流程（真机）

1. **设备** → 选择 **米家自动化**
2. **今晚** → **开启今晚**
3. 米家配置：睡着 → `tidesleep://sleep?state=asleep`；醒来 → `tidesleep://sleep?state=awake`
4. 详见 [小米穿戴与米家接入](docs/小米穿戴与米家接入.md)

### 小米穿戴 SDK

1. [dev.mi.com](https://dev.mi.com/) 申请，包名 `com.tidesleep.app`
2. AAR 放入 `android/app/libs/`
3. 重新编译，设备页选择 **小米穿戴 SDK**

---

## 许可证

待定（MVP 脚手架阶段）。
