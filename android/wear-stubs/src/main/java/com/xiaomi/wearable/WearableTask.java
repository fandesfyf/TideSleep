package com.xiaomi.wearable;

public interface WearableTask<T> {
    WearableTask<T> addOnSuccessListener(OnSuccessListener<T> listener);

    WearableTask<T> addOnFailureListener(OnFailureListener listener);
}
