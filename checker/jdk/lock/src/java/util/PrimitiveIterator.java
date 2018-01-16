package java.util;

import org.checkerframework.checker.lock.qual.*;

import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;

public interface PrimitiveIterator<T, T_CONS> extends Iterator<T> {
    void forEachRemaining(T_CONS arg0);

    public interface OfInt extends PrimitiveIterator<Integer,IntConsumer> {
        int nextInt();
        @Override
        void forEachRemaining(IntConsumer arg0);
        @Override
        Integer next(@GuardSatisfied OfInt this);
        void forEachRemaining(Consumer<? super Integer> arg0);
    }

    public interface OfLong extends PrimitiveIterator<Long,LongConsumer> {
        long nextLong();
        @Override
        void forEachRemaining(LongConsumer arg0);
        @Override
        Long next(@GuardSatisfied OfLong this);
        void forEachRemaining(Consumer<? super Long> arg0);
    }

    public interface OfDouble extends PrimitiveIterator<Double,DoubleConsumer> {
        double nextDouble();
        @Override
        void forEachRemaining(DoubleConsumer arg0);
        @Override
        Double next(@GuardSatisfied OfDouble this);
        void forEachRemaining(Consumer<? super Double> arg0);
    }
}
