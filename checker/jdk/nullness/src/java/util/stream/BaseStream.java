package java.util.stream;

import java.util.Iterator;
import java.util.Spliterator;

import org.checkerframework.dataflow.qual.SideEffectFree;

public interface BaseStream<T, S extends BaseStream<T, S>>
        extends AutoCloseable {
    @SideEffectFree
    Iterator<T> iterator();
    @SideEffectFree
    Spliterator<T> spliterator();
    boolean isParallel();
    S sequential();
    S parallel();
    S unordered();
    S onClose(Runnable closeHandler);
    @Override
    void close();
}
