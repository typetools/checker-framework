package java.util.stream;

import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;

import org.checkerframework.dataflow.qual.SideEffectFree;

public interface DoubleStream extends BaseStream<Double,DoubleStream> {
    DoubleStream filter(DoublePredicate arg0);
    DoubleStream map(DoubleUnaryOperator arg0);
    <U> Stream<U> mapToObj(DoubleFunction<? extends U> arg0);
    IntStream mapToInt(DoubleToIntFunction arg0);
    LongStream mapToLong(DoubleToLongFunction arg0);
    DoubleStream flatMap(DoubleFunction<? extends DoubleStream> arg0);
    DoubleStream distinct();
    DoubleStream sorted();
    DoubleStream peek(DoubleConsumer arg0);
    DoubleStream limit(long arg0);
    DoubleStream skip(long arg0);
    void forEach(DoubleConsumer arg0);
    void forEachOrdered(DoubleConsumer arg0);
    @SideEffectFree
    double[] toArray();
    double reduce(double arg0, DoubleBinaryOperator arg1);
    OptionalDouble reduce(DoubleBinaryOperator arg0);
    <R> R collect(Supplier<R> arg0, ObjDoubleConsumer<R> arg1, BiConsumer<R,R> arg2);
    double sum();
    OptionalDouble min();
    OptionalDouble max();
    long count();
    OptionalDouble average();
    DoubleSummaryStatistics summaryStatistics();
    boolean anyMatch(DoublePredicate arg0);
    boolean allMatch(DoublePredicate arg0);
    boolean noneMatch(DoublePredicate arg0);
    OptionalDouble findFirst();
    OptionalDouble findAny();
    Stream<Double> boxed();
    @Override
    DoubleStream sequential();
    @Override
    DoubleStream parallel();
    @Override
    @SideEffectFree
    PrimitiveIterator.OfDouble iterator();
    @Override
    @SideEffectFree
    Spliterator.OfDouble spliterator();

    static Builder builder() { throw new RuntimeException("skeleton method"); }
    static DoubleStream empty() { throw new RuntimeException("skeleton method"); }
    static DoubleStream of(double arg0) { throw new RuntimeException("skeleton method"); }
    static DoubleStream of(double[] arg0) { throw new RuntimeException("skeleton method"); }
    static DoubleStream iterate(double arg0, DoubleUnaryOperator arg1) { throw new RuntimeException("skeleton method"); }
    static DoubleStream generate(DoubleSupplier arg0) { throw new RuntimeException("skeleton method"); }
    static DoubleStream concat(DoubleStream arg0, DoubleStream arg1) { throw new RuntimeException("skeleton method"); }

    interface Builder extends DoubleConsumer {
        @Override
        void accept(double arg0);
        Builder add(double arg0);
        DoubleStream build();
    }
}
