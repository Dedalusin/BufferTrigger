package org.dedalusin.collection.impl;

import javax.annotation.Nullable;
import java.util.concurrent.locks.Condition;

public interface RejectHandler<T> {
    /**
     * return
     * true 确认element元素被消费reject
     * false 传递给上层继续执行enqueue
     */
    boolean onReject(T element, @Nullable Condition condition);
}
