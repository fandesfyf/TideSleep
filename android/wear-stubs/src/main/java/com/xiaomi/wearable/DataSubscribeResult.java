package com.xiaomi.wearable;

public interface DataSubscribeResult {

    int RESULT_CONNECTION_CONNECTED = 1;
    int RESULT_CONNECTION_DISCONNECTED = 2;

    int RESULT_SLEEP_ASLEEP = 1;
    int RESULT_SLEEP_AWAKE = 2;

    int getConnectedStatus();

    int getSleepStatus();
}
