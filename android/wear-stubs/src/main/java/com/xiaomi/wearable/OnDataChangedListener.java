package com.xiaomi.wearable;

public interface OnDataChangedListener {
    void onDataChanged(String nodeId, DataItem dataItem, DataSubscribeResult data);
}
