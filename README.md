# 汐眠（TideSleep）

> 等你睡着，再推一把慢波

**汐眠**是一款「**确认入睡后再播放**」的睡眠音频助手：用米家手表/手环判断入睡与出睡，在安全音量下以 **约 50 ms 粉红噪声短脉冲**（非整夜连续噪声）通过手机播放；远期接入 EEG 头环，逼近 MIT 闭环方案。

⚠️ **非医疗器械 / 非疗效承诺**：当前为开放环（open-loop）MVP，**无法**检测慢波相位，**不等于**论文级闭环听觉刺激。文案仅描述已发表生理机制，不宣称治疗失眠或预防痴呆。

---

## 产品要点

| 项 | 说明 |
|---|---|
| 触发 | 穿戴检测到「已入睡」后，经可配置延迟再开始刺激 |
| 刺激 | 稀疏 ~50 ms 粉红噪声（1/f）脉冲，间隔带随机抖动 |
| 停止 | 出睡即停 / 单晚时长上限 / 手动紧急停止 |
| 诚实边界 | 无 EEG ≠ 相位锁定；默认禁止整夜连续粉红噪声 |

研究与实施方案详见 [`docs/调研与实施方案.md`](docs/调研与实施方案.md)。  
UI 信息架构见 [`docs/UI设计与信息架构.md`](docs/UI设计与信息架构.md)。  
原型图：[`docs/prototypes/`](docs/prototypes/)。

---

## 架构（MVP）

```
小米手表/手环 ──入睡/出睡──► 汐眠 App（会话引擎 + 粉红噪声脉冲播放器）
                                 │
                          手机扬声器 / 蓝牙耳机
                          （P2：EEG 头环相位锁定）
```

**会话状态机：**  
`Idle → Arming → WaitingSleep → Stimulating → Paused/Stopped → MorningReport`

核心模块（`android/`，包名 `com.tidesleep.app`）：

- `session` — 状态机与安全策略（延迟、上限、出睡停）
- `audio.PinkNoisePulsePlayer` — ~50 ms 1/f 脉冲，非连续噪声
- `wearable.WearableSleepMonitor` — 接口；`FakeSleepMonitor` 演示；`XiaomiWearSleepMonitor` 桩（TODO：DEVICE_MANAGER 睡眠订阅）

---

## 构建（Android）

```bash
cd android
# 需本机 Android SDK；首次可复制 local.properties.example → local.properties
./gradlew :app:assembleDebug
```

- minSdk 26 / targetSdk 35 / compileSdk 35  
- Kotlin + Jetpack Compose  
- 若无 SDK，Gradle 结构仍完整，可在 Android Studio 打开同步。

### 演示模式

未接真机穿戴时，使用 `FakeSleepMonitor`：在「今晚」页可手动模拟入睡/出睡，验证脉冲与安全停播。

---

## 米家 / 小米穿戴说明

1. **无代码验证（最快）**：米家自动化 — 若穿戴「睡眠状态=睡着」→ 音箱播放；「醒来」→ 停止。用于测量判睡延迟。
2. **App 订阅（MVP 目标）**：小米穿戴第三方能力 `Permission.DEVICE_MANAGER`，query/subscribe 睡眠状态（入睡/出睡）。详见 `XiaomiWearSleepMonitor` 中 TODO。
3. **前置**：穿戴绑定小米运动健康；米家开启运动健康数据访问；HyperOS 2+ 手机和/或蓝牙 Mesh 网关。
4. **边界**：能判大致入睡/醒来；**不能**替代 EEG 做慢波相位检测。

App 内「科学与米家」页含向导与免责声明。

---

## 默认安全参数（可配置）

| 参数 | 默认 | 说明 |
|---|---|---|
| 脉冲时长 | 50 ms | 对齐论文短脉冲 |
| 频谱 | 粉红噪声 1/f | |
| 开播延迟 | 入睡后 15 min | 降低入睡干扰 |
| 会话上限 | 90 min | 偏向前半夜 |
| 脉冲间隔 | 4±2 s 抖动 | 开放环近似 |
| 停止 | 出睡 / 超时 / 手动 | 出睡优先 |

---

## 仓库结构

```
TideSleep/
├── README.md
├── docs/
│   ├── 调研与实施方案.md
│   ├── UI设计与信息架构.md
│   └── prototypes/          # UI 原型 PNG
└── android/                 # Kotlin + Compose 工程
```

## 许可与贡献

个人/早期脚手架；提交前请保留非医疗免责声明。Issues / PR 欢迎讨论米家联调与 EEG 路线。
