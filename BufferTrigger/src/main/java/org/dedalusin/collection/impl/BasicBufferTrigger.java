package org.dedalusin.collection.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BasicBufferTrigger {
    int maxBufferSize = 10;
    List<Integer> list;

    AtomicReference<List> buffer = new AtomicReference<>();

    ScheduledExecutorService scheduledExecutorService = createScheduleExecutor();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
            System.out.println("running");
            consume();
            scheduledExecutorService.schedule(this, 5, TimeUnit.SECONDS);
        }
    };

    public BasicBufferTrigger() {
        this.list = new ArrayList<>();
        this.buffer.set(list);
        this.scheduledExecutorService.schedule(runnable, 5, TimeUnit.SECONDS);
    }

    public void enqueue(int i) {
        List temp = buffer.get();
        if (temp.size() >= maxBufferSize) {
            handleReject(i);
        }
        temp.add(i);
    }

    public void consume() {
        List old = buffer.getAndSet(new ArrayList());
        old.forEach(System.out::println);
    }

    private void handleReject(int i) {
        System.out.println("reject : " + i);
    }

    private ScheduledExecutorService createScheduleExecutor() {
        String threadPattern = "pool-basic-bugger-trigger-%d";
        return Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat(threadPattern)
                        .setDaemon(true)
                        .build()
        );
    }

    public static void main(String[] args) {
        BasicBufferTrigger basicBufferTrigger = new BasicBufferTrigger();
        for (int i = 0; i < 20; i++) {
            basicBufferTrigger.enqueue(i);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }



}
