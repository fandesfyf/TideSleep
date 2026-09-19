# 小米穿戴官方 SDK AAR

汐眠通过 **可选** 本地 AAR 接入小米穿戴第三方开放能力（睡眠 query/subscribe），SDK **不在公共 Maven** 发布。

## 获取 AAR

1. 登录 [HyperOS 开放平台](https://dev.mi.com/)（原小米开放平台）
2. 创建应用，包名填写 **`com.tidesleep.app`**
3. 申请「小米穿戴第三方 App 能力」并通过审核
4. 下载官方提供的 wearable AAR（文档 v1.4）
5. 将 AAR 文件放入本目录 `android/app/libs/`

## 启用编译

任选其一：

- **自动**：`libs/` 下存在 `*.aar` 时 Gradle 自动 `implementation`
- **手动**：在 `android/gradle.properties` 增加 `tidesleep.xiaomiSdk=true`（即使尚未放入 AAR，也仅在有文件时真正链接）

无 AAR 时工程仍可 `./gradlew assembleDebug`；运行时在「设备 → 小米穿戴 SDK」会提示 **未找到小米穿戴 SDK AAR**，请改用 **米家自动化** 路径。

## 签名指纹

开放平台需填写应用 **SHA1 / SHA256** 指纹。调试包可用：

```bash
cd android
./gradlew :app:signingReport
```

Release 请使用正式 keystore 的指纹。

## 权限

真机集成需向用户申请：

- `Permission.DEVICE_MANAGER` — 查询/订阅睡眠与连接状态
- `Permission.NOTIFY` — 可选，手表消息通知

详见仓库 `docs/小米穿戴与米家接入.md`。
