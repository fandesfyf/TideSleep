package com.xiaomi.wearable;

import android.content.Context;

/**
 * Compile-only stub mirroring 小米穿戴第三方 SDK v1.4.
 * Replace with official AAR from HyperOS open platform at runtime.
 */
public final class Wearable {

    private Wearable() {}

    public static NodeApi getNodeApi(Context context) {
        throw new UnsupportedOperationException("Stub: drop official wearable AAR into app/libs/");
    }

    public static AuthApi getAuthApi(Context context) {
        throw new UnsupportedOperationException("Stub: drop official wearable AAR into app/libs/");
    }
}
