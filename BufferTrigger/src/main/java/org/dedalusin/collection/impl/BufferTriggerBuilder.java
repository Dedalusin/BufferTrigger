package org.dedalusin.collection.impl;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chenchen30 <chenchen30@kuaishou.com>
 * Created on 2023-01-05
 */
public class BufferTriggerBuilder<C, E> {
    private static final Logger logger = LoggerFactory.getLogger(BufferTriggerBuilder.class);
    LongSupplier maxBufferSize = () -> 1L;
    Supplier<C> bufferFactory;
    AtomicReference<C> buffer;
    Function<C, E> bufferAdder;

    public LongSupplier getMaxBufferSize() {
        return maxBufferSize;
    }

    public BufferTriggerBuilder setMaxBufferSize(LongSupplier maxBufferSize) {
        this.maxBufferSize = maxBufferSize;
        return this;
    }

    public Supplier<C> getBufferFactory() {
        return bufferFactory;
    }

    public BufferTriggerBuilder setBufferFactory(Supplier<C> bufferFactory) {
        this.bufferFactory = bufferFactory;
        return this;
    }

    public AtomicReference<C> getBuffer() {
        return buffer;
    }

    public BufferTriggerBuilder setBuffer(AtomicReference<C> buffer) {
        this.buffer = buffer;
        return this;
    }

    public Function<C, E> getBufferAdder() {
        return bufferAdder;
    }

    public BufferTriggerBuilder setBufferAdder(Function<C, E> bufferAdder) {
        this.bufferAdder = bufferAdder;
        return this;
    }
}
