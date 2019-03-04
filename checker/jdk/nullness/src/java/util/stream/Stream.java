package java.util.stream;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.UnaryOperator;

import org.checkerframework.checker.nullness.qual.PolyNull;
import org.checkerframework.dataflow.qual.SideEffectFree;

public interface Stream<T> extends BaseStream<T,Stream<T>> {
    Stream<T> filter(Predicate<? super T> arg0);
    <R> Stream<R> map(Function<? super T,? extends R> arg0);
    IntStream mapToInt(ToIntFunction<? super T> arg0);
    LongStream mapToLong(ToLongFunction<? super T> arg0);
    DoubleStream mapToDouble(ToDoubleFunction<? super T> arg0);
    <R> Stream<R> flatMap(Function<? super T,? extends Stream<? extends R>> arg0);
    IntStream flatMapToInt(Function<? super T,? extends IntStream> arg0);
    LongStream flatMapToLong(Function<? super T,? extends LongStream> arg0);
    DoubleStream flatMapToDouble(Function<? super T,? extends DoubleStream> arg0);
    Stream<T> distinct();
    Stream<T> sorted();
    Stream<T> sorted(Comparator<? super T> arg0);
    Stream<T> peek(Consumer<? super T> arg0);
    Stream<T> limit(long arg0);
    Stream<T> skip(long arg0);
    void forEach(Consumer<? super T> arg0);
    void forEachOrdered(Consumer<? super T> arg0);
    @SideEffectFree
    public @PolyNull Object[] toArray(Stream<@PolyNull T> this);
    @SideEffectFree
    <A> A[] toArray(IntFunction<A[]> arg0);
    T reduce(T arg0, BinaryOperator<T> arg1);
    Optional<T> reduce(BinaryOperator<T> arg0);
    <U> U reduce(U arg0, BiFunction<U,? super T,U> arg1, BinaryOperator<U> arg2);
    <R> R collect(Supplier<R> arg0, BiConsumer<R,? super T> arg1, BiConsumer<R,R> arg2);
    <R, A> R collect(Collector<? super T,A,R> arg0);
    Optional<T> min(Comparator<? super T> arg0);
    Optional<T> max(Comparator<? super T> arg0);
    long count();
    boolean anyMatch(Predicate<? super T> arg0);
    boolean allMatch(Predicate<? super T> arg0);
    boolean noneMatch(Predicate<? super T> arg0);
    Optional<T> findFirst();
    Optional<T> findAny();

    static <T> Builder<T> builder() { throw new RuntimeException("skeleton method"); }
    static <T> Stream<T> empty() { throw new RuntimeException("skeleton method"); }
    static <T> Stream<T> of(T arg0) { throw new RuntimeException("skeleton method"); }
    static <T> Stream<T> of(T[] arg0) { throw new RuntimeException("skeleton method"); }
    static <T> Stream<T> iterate(T arg0, UnaryOperator<T> arg1) { throw new RuntimeException("skeleton method"); }
    static <T> Stream<T> generate(Supplier<T> arg0) { throw new RuntimeException("skeleton method"); }
    static <T> Stream<T> concat(Stream<? extends T> arg0, Stream<? extends T> arg1) { throw new RuntimeException("skeleton method"); }

    interface Builder<T> extends Consumer<T> {
        @Override
        void accept(T arg0);
        Builder<T> add(T arg0);
        Stream<T> build();
    }

}
