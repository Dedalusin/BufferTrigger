package org.dedalusin.collection.impl;

import org.dedalusin.collection.BufferTrigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
    RejectHandler<E> rejectHandler;
    Consumer<C> consumer;
    Runnable shutdownExecutor;

    ReentrantReadWriteLock.ReadLock readLock;

    ReentrantReadWriteLock.WriteLock writeLock;

    Condition writeCondition;

    AtomicLong counter = new AtomicLong();

    @Override
    public void enqueue(E e) {
        if (counter.get() >= maxBufferSize.getAsLong()) {
            // 默认通过主要是为了防止某些异常情况时，遗漏消费
            boolean isPass = true;
            if (rejectHandler != null && writeLock != null) {
                try {
                    writeLock.lock();
                    if (counter.get() >= maxBufferSize.getAsLong()) {
                        isPass = !rejectHandler.onReject(e, writeCondition);
                    }
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    writeLock.unlock();
                }
            }
            if (!isPass) {
                return;
            }
        }
        try {
            if (readLock != null) {
                readLock.lock();
            }
            if (bufferAdder.test(buffer.get(), e)) {
                counter.addAndGet(1);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            if (readLock != null) {
                readLock.unlock();
            }
        }

    }

    @Override
    public void manualDoTrigger() {
        doConsume();
    }

    /**
     * 临界变量有两个，1：buffer 2：counter
     * 虽然这两个的操作自身都是原子的，但两个合起来的事物却是非原子的，所以会出现不一致，因而需要加锁
     */
    private void doConsume() {
        C old = null;
        try {
            if (writeLock != null) {
                writeLock.lock();
            }
            old = buffer.getAndSet(bufferFactory.get());
            counter.set(0L);
            if (writeCondition != null) {
                writeCondition.signalAll();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writeLock != null) {
                writeLock.unlock();
            }
        }
        consumer.accept(old);
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
        if (!builder.disableSwitchLock) {
            ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
            readLock = lock.readLock();
            writeLock = lock.writeLock();
            writeCondition = writeLock.newCondition();
        } else {
            readLock = null;
            writeLock = null;
            writeCondition = null;
        }
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
