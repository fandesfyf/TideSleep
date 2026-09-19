package com.xiaomi.wearable;

public interface AuthApi {
    WearableTask<Boolean> checkPermission(String nodeId, Permission permission);

    WearableTask<Boolean[]> checkPermissions(String nodeId, Permission[] permissions);

    WearableTask<Permission[]> requestPermission(String nodeId, Permission... permissions);
}
