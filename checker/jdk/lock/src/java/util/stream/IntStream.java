package java.util.stream;

import org.checkerframework.checker.lock.qual.GuardSatisfied;
import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

public interface IntStream extends BaseStream<Integer,IntStream> {
    IntStream filter(IntPredicate arg0);
    IntStream map(IntUnaryOperator arg0);
    <U> Stream<U> mapToObj(IntFunction<? extends U> arg0);
    LongStream mapToLong(IntToLongFunction arg0);
    DoubleStream mapToDouble(IntToDoubleFunction arg0);
    IntStream flatMap(IntFunction<? extends IntStream> arg0);
    IntStream distinct();
    IntStream sorted();
    IntStream peek(IntConsumer arg0);
    IntStream limit(long arg0);
    IntStream skip(long arg0);
    void forEach(IntConsumer arg0);
    void forEachOrdered(IntConsumer arg0);
    int[] toArray();
    int reduce(int arg0, IntBinaryOperator arg1);
    OptionalInt reduce(IntBinaryOperator arg0);
    <R> R collect(Supplier<R> arg0, ObjIntConsumer<R> arg1, BiConsumer<R,R> arg2);
    int sum();
    OptionalInt min();
    OptionalInt max();
    long count();
    OptionalDouble average();
    IntSummaryStatistics summaryStatistics();
    boolean anyMatch(IntPredicate arg0);
    boolean allMatch(IntPredicate arg0);
    boolean noneMatch(IntPredicate arg0);
    OptionalInt findFirst();
    OptionalInt findAny();
    LongStream asLongStream();
    DoubleStream asDoubleStream();
    Stream<Integer> boxed();
    @Override
    IntStream sequential();
    @Override
    IntStream parallel();
    @Override
    PrimitiveIterator.OfInt iterator();
    @Override
    Spliterator.OfInt spliterator();
    static Builder builder() { throw new RuntimeException("skeleton method"); }
    static IntStream empty() { throw new RuntimeException("skeleton method"); }
    static IntStream of(int arg0) { throw new RuntimeException("skeleton method"); }
    static IntStream of(int[] arg0) { throw new RuntimeException("skeleton method"); }
    static IntStream iterate(int arg0, IntUnaryOperator arg1) { throw new RuntimeException("skeleton method"); }
    static IntStream generate(IntSupplier arg0) { throw new RuntimeException("skeleton method"); }
    static IntStream range(int arg0, int arg1) { throw new RuntimeException("skeleton method"); }
    static IntStream rangeClosed(int arg0, int arg1) { throw new RuntimeException("skeleton method"); }
    static IntStream concat(IntStream arg0, IntStream arg1) { throw new RuntimeException("skeleton method"); }

    interface Builder extends IntConsumer {
        @Override
        void accept(int arg0);
        Builder add(@GuardSatisfied Builder this, int arg0);
        IntStream build();
    }
}
