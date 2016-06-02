package java.util;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public interface Spliterator<T> {
    static final int ORDERED = -1;
    static final int DISTINCT = -1;
    static final int SORTED = -1;
    static final int SIZED = -1;
    static final int NONNULL = -1;
    static final int IMMUTABLE = -1;
    static final int CONCURRENT = -1;
    static final int SUBSIZED = -1;

    boolean tryAdvance(Consumer<? super T> arg0);
    void forEachRemaining(Consumer<? super T> arg0);
    Spliterator<T> trySplit();
    long estimateSize();
    long getExactSizeIfKnown();
    int characteristics();
    boolean hasCharacteristics(int arg0);
    Comparator<? super T> getComparator();

    public interface OfPrimitive<T, T_CONS, T_SPLITR extends Spliterator.OfPrimitive<T, T_CONS, T_SPLITR>> extends Spliterator<T> {
        @Override
        T_SPLITR trySplit();
        boolean tryAdvance(T_CONS arg0);
        void forEachRemaining(T_CONS arg0);
    }

    public interface OfInt extends OfPrimitive<Integer,IntConsumer,OfInt> {
        @Override
        OfInt trySplit();
        @Override
        boolean tryAdvance(IntConsumer arg0);
        @Override
        void forEachRemaining(IntConsumer arg0);
        @Override
        boolean tryAdvance(Consumer<? super Integer> arg0);
        @Override
        void forEachRemaining(Consumer<? super Integer> arg0);
    }

    public interface OfLong extends OfPrimitive<Long,LongConsumer,OfLong> {
        @Override
        OfLong trySplit();
        @Override
        boolean tryAdvance(LongConsumer arg0);
        @Override
        void forEachRemaining(LongConsumer arg0);
        @Override
        boolean tryAdvance(Consumer<? super Long> arg0);
        @Override
        void forEachRemaining(Consumer<? super Long> arg0);
    }

    public interface OfDouble extends OfPrimitive<Double,DoubleConsumer,OfDouble> {
        @Override
        OfDouble trySplit();
        @Override
        boolean tryAdvance(DoubleConsumer arg0);
        @Override
        void forEachRemaining(DoubleConsumer arg0);
        @Override
        boolean tryAdvance(Consumer<? super Double> arg0);
        @Override
        void forEachRemaining(Consumer<? super Double> arg0);
    }
}
