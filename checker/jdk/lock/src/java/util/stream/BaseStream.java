package java.util.stream;

import java.util.Iterator;
import java.util.Spliterator;

public interface BaseStream<T, S extends BaseStream<T, S>>
        extends AutoCloseable {
    Iterator<T> iterator();
    Spliterator<T> spliterator();
    boolean isParallel();
    S sequential();
    S parallel();
    S unordered();
    S onClose(Runnable closeHandler);
    @Override
    void close();
}
