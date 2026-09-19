package com.xiaomi.wearable;

public interface NodeApi {
    WearableTask<java.util.List<Node>> getConnectedNodes();

    WearableTask<DataQueryResult> query(String nodeId, DataItem dataItem);

    WearableTask<Void> subscribe(String nodeId, DataItem dataItem, OnDataChangedListener listener);

    WearableTask<Void> unsubscribe(String nodeId, DataItem dataItem);
}
