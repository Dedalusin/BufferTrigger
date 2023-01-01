package org.dedalusin.collection;

public interface BufferTrigger<E> extends AutoCloseable{
    void enqueue(E e);

    void manualDoTrigger();
}
