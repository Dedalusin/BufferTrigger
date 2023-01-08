package org.dedalusin.collection.impl;

import org.dedalusin.collection.BufferTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static com.google.common.util.concurrent.MoreExecutors.shutdownAndAwaitTermination;

public class SimpleBufferTrigger<E, C> implements BufferTrigger<E> {
    private static final Logger logger = LoggerFactory.getLogger(SimpleBufferTriggerBuilder.class);
    LongSupplier maxBufferSize = () -> 1L;
    LongSupplier interval = () -> 1000L;
    Supplier<C> bufferFactory;
    AtomicReference<C> buffer;
    BiPredicate<C, E> bufferAdder;
    Consumer<E> rejectHandler;
    Consumer<C> consumer;
    Runnable shutdownExecutor;


    AtomicLong counter = new AtomicLong();

    @Override
    public void enqueue(E e) {
        if (counter.get() >= maxBufferSize.getAsLong()) {
            rejectHandler.accept(e);
            return;
        }
        if (bufferAdder.test(buffer.get(), e)) {
            counter.addAndGet(1);
        }
    }

    @Override
    public void manualDoTrigger() {
        doConsume();
    }

    private void doConsume() {
        C old = buffer.getAndSet(bufferFactory.get());
        consumer.accept(old);
        counter.set(0L);
    }

    @Override
    public void close() throws Exception {
        manualDoTrigger();
        shutdownExecutor.run();
    }

    SimpleBufferTrigger(SimpleBufferTriggerBuilder<C, E> builder) {
        this.maxBufferSize = builder.maxBufferSize;
        this.bufferFactory = builder.bufferFactory;
        this.bufferAdder = builder.bufferAdder;
        this.buffer = new AtomicReference<>(this.bufferFactory.get());
        this.rejectHandler = builder.rejectHandler;
        this.consumer = builder.consumer;
        this.interval = builder.interval;
        builder.scheduledExecutorService.schedule(new Runnable() {
            @Override
            public void run() {
                doConsume();
                System.out.println("-----trig--------");
                builder.scheduledExecutorService.schedule(this, interval.getAsLong(), TimeUnit.MILLISECONDS);
            }
        }, interval.getAsLong(), TimeUnit.MILLISECONDS);
        this.shutdownExecutor = () -> {
            if (builder.isInnerExecutor) {
                // 只有当为内部executor时才关闭，外部依赖会造成溢出影响
                shutdownAndAwaitTermination(builder.scheduledExecutorService, 1, TimeUnit.DAYS);
            }
        };
    }

    public static SimpleBufferTriggerBuilder<Object, Object> newBuilder() {
        return new SimpleBufferTriggerBuilder<>();
    }
}
