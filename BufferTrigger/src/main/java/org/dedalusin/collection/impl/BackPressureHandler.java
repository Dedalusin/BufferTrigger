package org.dedalusin.collection.impl;

import javax.annotation.Nullable;
import java.util.concurrent.locks.Condition;

public class BackPressureHandler<T> implements RejectHandler<T> {
    BackPressureListener<T> backPressureListener;

    public BackPressureHandler(BackPressureListener<T> backPressureListener) {
        this.backPressureListener = backPressureListener;
    }

    @Override
    public boolean onReject(T element, @Nullable Condition condition) {
        if (this.backPressureListener != null) {
            backPressureListener.onHandle(element);
        }
        if (condition != null) {
            condition.awaitUninterruptibly();
        }
        return false;
    }
}
