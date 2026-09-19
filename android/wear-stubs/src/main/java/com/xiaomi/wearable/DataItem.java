package com.xiaomi.wearable;

public final class DataItem {

    public static final DataItem ITEM_CONNECTION = new DataItem(1);
    public static final DataItem ITEM_SLEEP = new DataItem(5);

    private final int type;

    private DataItem(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }
}
