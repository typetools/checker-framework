package java.util.stream;

import org.checkerframework.checker.lock.qual.GuardSatisfied;
import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;

public interface LongStream extends BaseStream<Long,LongStream> {
    LongStream filter(LongPredicate arg0);
    LongStream map(LongUnaryOperator arg0);
    <U> Stream<U> mapToObj(LongFunction<? extends U> arg0);
    IntStream mapToInt(LongToIntFunction arg0);
    DoubleStream mapToDouble(LongToDoubleFunction arg0);
    LongStream flatMap(LongFunction<? extends LongStream> arg0);
    LongStream distinct();
    LongStream sorted();
    LongStream peek(LongConsumer arg0);
    LongStream limit(long arg0);
    LongStream skip(long arg0);
    void forEach(LongConsumer arg0);
    void forEachOrdered(LongConsumer arg0);
    long[] toArray();
    long reduce(long arg0, LongBinaryOperator arg1);
    OptionalLong reduce(LongBinaryOperator arg0);
    <R> R collect(Supplier<R> arg0, ObjLongConsumer<R> arg1, BiConsumer<R,R> arg2);
    long sum();
    OptionalLong min();
    OptionalLong max();
    long count();
    OptionalDouble average();
    LongSummaryStatistics summaryStatistics();
    boolean anyMatch(LongPredicate arg0);
    boolean allMatch(LongPredicate arg0);
    boolean noneMatch(LongPredicate arg0);
    OptionalLong findFirst();
    OptionalLong findAny();
    DoubleStream asDoubleStream();
    Stream<Long> boxed();
    @Override
    LongStream sequential();
    @Override
    LongStream parallel();
    @Override
    PrimitiveIterator.OfLong iterator();
    @Override
    Spliterator.OfLong spliterator();

    static Builder builder() { throw new RuntimeException("skeleton method"); }
    static LongStream empty() { throw new RuntimeException("skeleton method"); }
    static LongStream of(long arg0) { throw new RuntimeException("skeleton method"); }
    static LongStream of(long[] arg0) { throw new RuntimeException("skeleton method"); }
    static LongStream iterate(long arg0, LongUnaryOperator arg1) { throw new RuntimeException("skeleton method"); }
    static LongStream generate(LongSupplier arg0) { throw new RuntimeException("skeleton method"); }
    static LongStream range(long arg0, long arg1) { throw new RuntimeException("skeleton method"); }
    static LongStream rangeClosed(long arg0, long arg1) { throw new RuntimeException("skeleton method"); }
    static LongStream concat(LongStream arg0, LongStream arg1) { throw new RuntimeException("skeleton method"); }

    interface Builder extends LongConsumer {
        @Override
        void accept(long arg0);
        Builder add(@GuardSatisfied Builder this, long arg0);
        LongStream build();
    }
}
