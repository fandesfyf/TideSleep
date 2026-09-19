# 汐眠 TideSleep

入睡后粉红噪声脉冲助眠；米家手表检测入睡后播放；基于慢波 / CSF 研究的开放环 MVP。

> 无 EEG 时为开放环（入睡触发），不等于论文级相位闭环。本项目不提供医疗诊断或治疗建议。

---

## 产品简介

**汐眠（TideSleep）** 是一款「确认入睡后再播放」的睡眠音频助手：

- 由小米手表/手环判断入睡与出睡
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

## 架构概览

```
小米手表/手环 ──入睡/出睡──► 汐眠 App（会话引擎 + 音频引擎）
                                    │
                                    ▼
                              手机扬声器（稀疏脉冲）
```

**会话状态机**：`Idle → Arming → WaitingSleep → Stimulating → Stopped`

| 模块 | 路径 | 说明 |
|------|------|------|
| 会话引擎 | `session/SessionEngine.kt` | 延迟开播、超时、出睡即停 |
| 音频 | `audio/PinkNoisePulsePlayer.kt` | 50 ms 1/f 粉红噪声合成 |
| 穿戴 | `wearable/` | `FakeSleepMonitor`（演示）+ `XiaomiWearSleepMonitor`（Stub） |
| UI | `ui/screens/` | Compose 四大 Tab + 会话页 |

详见 [UI 设计与信息架构](docs/UI设计与信息架构.md) 与原型图 `docs/prototypes/`。

---

## 仓库结构

```
.
├── README.md
├── docs/
│   ├── 调研与实施方案.md
│   ├── UI设计与信息架构.md
│   └── prototypes/          # UI 原型 PNG
└── android/                 # Android 工程（com.tidesleep.app）
    └── app/src/main/kotlin/com/tidesleep/app/
```

---

## 构建与运行

### 环境要求

- **Android Studio** Ladybug (2024.2) 或更高
- **JDK 17+**
- **Android SDK 35**（`compileSdk` / `targetSdk`）
- **minSdk 26**

当前 Cloud Agent 环境未预装 Android SDK，无法在 CI 中完成编译；请在本地 Android Studio 打开 `android/` 目录构建。

### 本地构建

```bash
cd android
./gradlew assembleDebug
```

安装到设备：

```bash
./gradlew installDebug
```

### 演示流程（Fake 模式）

1. 打开 App → **今晚** → 点击「今晚就寝」
2. 切到 **设备** Tab → 点击「模拟入睡」
3. 等待配置的入睡延迟（默认 15 分钟，可调短以测试）→ 进入稀疏脉冲
4. 点击「模拟醒来」或「立即停止」→ 会话结束

---

## 米家 / 小米穿戴前置条件

使用真实穿戴前，请确认：

1. 手环/手表已绑定 **小米运动健康**
2. **米家 App** ≥ 10.0，已开启「人车家数据访问管理」中的运动健康数据
3. 穿戴设备出现在米家设备列表
4. 上报通道：**HyperOS 2+** 小米手机，或 **蓝牙 Mesh 网关**（部分小爱音箱不支持）
5. App 内集成小米穿戴第三方 SDK 时需 `Permission.DEVICE_MANAGER`（当前为 Stub）

米家无代码验证步骤见 App 内「科学说明与米家向导」或 [调研文档 §4.4](docs/调研与实施方案.md)。

---

## 默认安全参数

| 参数 | 默认值 |
|------|--------|
| 入睡后延迟 | 15 分钟 |
| 单晚最长刺激 | 90 分钟 |
| 脉冲时长 | 50 ms 粉红噪声 |
| 脉冲间隔 | 4 ± 2 秒随机抖动 |
| 停止条件 | 出睡 / 超时 / 手动 |

---

## 许可证

待定（MVP 脚手架阶段）。
