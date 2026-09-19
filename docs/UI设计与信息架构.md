# 汐眠（TideSleep）UI 设计与信息架构

> 版本：v0.1｜日期：2026-09-19  
> 视觉基调：深海军蓝 + 靛蓝强调色；小米穿戴设备（非 Apple Watch）

---

## 1. 底部导航（四大 Tab）

| Tab | 路由 | 主要功能 |
|-----|------|----------|
| **今晚** | `tonight` | 一键就寝、会话状态、进入活跃会话页 |
| **设备** | `devices` | 小米手表/手环连接状态、Fake 演示开关 |
| **记录** | `records` | 本夜时间线、历史会话摘要（MVP 占位） |
| **我的** | `profile` | 安全设置、科学说明、米家向导、关于 |

原型图见 `docs/prototypes/`：

- `ximian-home.png` — 今晚首页
- `ximian-devices.png` — 设备页
- `ximian-settings.png` — 安全设置
- `ximian-session.png` — 会话进行中
- `ximian-science.png` — 科学与米家向导

---

## 2. 会话状态机

```
                    ┌─────────┐
                    │  Idle   │  未开启就寝
                    └────┬────┘
                         │ 用户点击「今晚就寝」
                         ▼
                    ┌─────────┐
                    │ Arming  │  初始化监听、前台服务
                    └────┬────┘
                         │ 监听就绪
                         ▼
               ┌──────────────────┐
               │ WaitingSleep     │  等待穿戴「入睡」事件，不播放
               └────────┬─────────┘
                        │ 入睡确认
                        ▼
               ┌──────────────────┐
               │ Stimulating      │  延迟后稀疏粉红噪声脉冲
               └────────┬─────────┘
          ┌──────────────┼──────────────┐
          │              │              │
    出睡/超时/手动      │              │
          ▼              ▼              ▼
     ┌─────────┐    (Paused 可选)   MorningReport
     │ Stopped │                      (P1)
     └─────────┘
```

### 状态说明

| 状态 | UI 表现 | 音频行为 |
|------|---------|----------|
| `Idle` | 大按钮「今晚就寝」、免责声明 | 无 |
| `Arming` | 加载指示、「准备监听…」 | 无 |
| `WaitingSleep` | 「等待入睡…」、设备状态 | 无 |
| `Stimulating` | 脉冲计数、剩余时长、一键停止 | 50 ms 粉红噪声脉冲 |
| `Stopped` | 停止原因、本夜摘要 | 静音 |

---

## 3. 页面清单与信息架构

### 3.1 TonightHome（今晚）

- 品牌标题「汐眠」
- 开放环诚实说明（一行）
- 主 CTA：「今晚就寝」/「停止今晚」
- 当前状态卡片（状态机 + 设备摘要）
- 快捷入口：安全设置、科学说明

### 3.2 Devices（设备）

- 已连接穿戴列表（MVP：Fake / 小米占位）
- 连接状态、睡眠状态订阅状态
- **演示模式**：FakeSleepMonitor 手动触发「模拟入睡」「模拟醒来」

### 3.3 SessionActive（会话进行中）

- 全屏深色会话视图
- 实时状态：WaitingSleep / Stimulating
- 脉冲已播放次数、会话剩余时间
- 醒目「立即停止」按钮

### 3.4 SafetySettings（安全设置）

| 参数 | 默认 | 范围 |
|------|------|------|
| 入睡后延迟 | 15 min | 5–30 min |
| 单晚最长刺激 | 90 min | 30–120 min |
| 脉冲时长 | 50 ms | 固定 |
| 脉冲间隔 | 4±2 s | 2–8 s |
| 音量上限 | 低 | 试听校准（P1） |

### 3.5 Records（记录）

- 本夜时间线：就寝开启 → 入睡 → 开始刺激 → 停止原因
- 历史列表占位（MVP）

### 3.6 ScienceAndMiJiaGuide（科学与米家）

- 论文机制摘要（慢波 → CSF）
- **诚实边界**：无 EEG ≠ 论文闭环
- 米家自动化配置步骤（图文引导）

### 3.7 About（关于）

- 版本号、开源说明
- 非医疗器械免责声明
- 隐私：睡眠状态本地处理

---

## 4. 设计 Token（Compose Theme）

| Token | 值 | 用途 |
|-------|-----|------|
| `Background` | `#0D1117` | 页面底色 |
| `Surface` | `#161B22` | 卡片 |
| `Primary` | `#6366F1` | 靛蓝强调、主按钮 |
| `PrimaryVariant` | `#818CF8` | 次要强调 |
| `OnBackground` | `#E6EDF3` | 主文字 |
| `OnSurfaceMuted` | `#8B949E` | 辅助文字 |
| `Success` | `#3FB950` | 已连接 |
| `Warning` | `#D29922` | 等待/警告 |

---

## 5. 导航流

```
MainActivity
  └── TideSleepNavHost (BottomBar: 今晚 | 设备 | 记录 | 我的)
        ├── TonightHome ──► SessionActive (when session active)
        ├── Devices
        ├── Records
        └── Profile
              ├── SafetySettings
              ├── ScienceAndMiJiaGuide
              └── About
```

---

## 6. MVP 范围外（P1+）

- 小爱音箱 IoT 控制
- 周报与可穿戴深睡占比
- 首次强制音量试听
- 真实小米 Wear SDK 集成（当前为 Stub）
