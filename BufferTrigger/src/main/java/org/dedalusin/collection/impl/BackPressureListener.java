package org.dedalusin.collection.impl;

public interface BackPressureListener<T> {
    void onHandle(T element);
}
