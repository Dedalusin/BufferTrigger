import org.dedalusin.collection.impl.SimpleBufferTrigger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

public class Test {
    Consumer<List<Integer>> consumer = (list) -> {
        list.forEach(System.out::println);
    };
    BiPredicate<List<Integer>, Integer> adder = List::add;
    SimpleBufferTrigger simpleBufferTrigger = SimpleBufferTrigger.newBuilder()
            .setBufferFactory(ArrayList::new)
            .setConsumer(consumer)
            .setInterval(() -> 5000L)
            .setRejectHandler((e) -> {
                System.out.println("reject" + e);
            })
            .setBufferAdder(adder)
            .setMaxBufferSize(() -> 8L)
            .build();
    @org.junit.jupiter.api.Test
    public void test() {
        System.out.println("ccc");
    }

    @org.junit.jupiter.api.Test
    public void simpleBufferTriggerTest() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 20; i++) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    simpleBufferTrigger.enqueue(1);
                }
            });
            Thread.sleep(500L);
        }
        System.out.println("~~~~~~~~");
        Thread.sleep(30000L);
    }

}
