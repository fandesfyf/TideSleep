package com.tidesleep.app.wearable

/**
 * 运行时检测官方小米穿戴 AAR 是否已打入 APK。
 * compileOnly 使用 wear-stubs；implementation(fileTree("libs")) 打入真 AAR 后本检测为 true。
 */
object XiaomiWearSdk {

    private val runtimeAvailable: Boolean by lazy {
        runCatching {
            val clazz = Class.forName("com.xiaomi.wearable.Wearable")
            val method = clazz.getMethod("getNodeApi", android.content.Context::class.java)
            method.returnType.name.contains("NodeApi")
        }.getOrDefault(false)
    }

    fun isAvailable(): Boolean = runtimeAvailable

    const val MISSING_AAR_MESSAGE = "未找到小米穿戴 SDK AAR"
}
