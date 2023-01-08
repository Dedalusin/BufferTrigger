package org.dedalusin.collection.impl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.*;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chenchen30 <chenchen30@kuaishou.com>
 * Created on 2023-01-05
 */
public class SimpleBufferTriggerBuilder<C, E> {
    private static final Logger logger = LoggerFactory.getLogger(SimpleBufferTriggerBuilder.class);
    LongSupplier maxBufferSize = () -> 1L;
    LongSupplier interval = () -> 1000L;
    Supplier<C> bufferFactory;
    BiPredicate<C, E> bufferAdder;
    Consumer<E> rejectHandler;
    Consumer<C> consumer;
    boolean isInnerExecutor = false;

    boolean disableSwitchLock = false;

    public SimpleBufferTriggerBuilder setDisableSwitchLock(boolean disableSwitchLock) {
        this.disableSwitchLock = disableSwitchLock;
        return this;
    }

    ScheduledExecutorService scheduledExecutorService;

    public SimpleBufferTriggerBuilder setInterval(LongSupplier interval) {
        this.interval = interval;
        return this;
    }

    public SimpleBufferTriggerBuilder setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
        return this;
    }

    public SimpleBufferTriggerBuilder setBufferAdder(BiPredicate<C, E> bufferAdder) {
        this.bufferAdder = bufferAdder;
        return this;
    }

    public SimpleBufferTriggerBuilder setConsumer(Consumer<C> consumer) {
        this.consumer = consumer;
        return this;
    }

    public SimpleBufferTriggerBuilder setRejectHandler(Consumer<E> rejectHandler) {
        this.rejectHandler = rejectHandler;
        return this;
    }

    public LongSupplier getMaxBufferSize() {
        return maxBufferSize;
    }

    public SimpleBufferTriggerBuilder setMaxBufferSize(LongSupplier maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
        return this;
    }

    public Supplier<C> getBufferFactory() {
        return bufferFactory;
    }

    public SimpleBufferTriggerBuilder setBufferFactory(Supplier<C> bufferFactory) {
        this.bufferFactory = bufferFactory;
        return this;
    }

    public SimpleBufferTrigger build() {
        ensure();
        return new SimpleBufferTrigger<E, C>(this);
    }
    private void ensure() {
        if (this.scheduledExecutorService == null) {
            this.scheduledExecutorService = makeScheduleExecutorService();
            this.isInnerExecutor = true;
        }
        if (rejectHandler == null) {
            this.rejectHandler = (e) -> {};
        }
        if (consumer == null) {
            throw new RuntimeException("should set consumer");
        }
        if (bufferAdder == null) {
            throw new RuntimeException("should set bufferAdder");
        }
    }

    private ScheduledExecutorService makeScheduleExecutorService() {
        String namePattern = "pool-simple-buffer-trigger-thread-%d";
        return Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setNameFormat(namePattern)
                        .setDaemon(true)
                        .build());
    }
}
