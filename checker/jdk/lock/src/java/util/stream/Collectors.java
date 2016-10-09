package java.util.stream;

import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.IntSummaryStatistics;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public class Collectors {
    public static <T, C> Collector<T,?,C> toCollection(Supplier<C> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,List<T>> toList() { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Set<T>> toSet() { throw new RuntimeException("skeleton method"); }
    public static Collector<CharSequence,?,String> joining() { throw new RuntimeException("skeleton method"); }
    public static Collector<CharSequence,?,String> joining(CharSequence arg0) { throw new RuntimeException("skeleton method"); }
    public static Collector<CharSequence,?,String> joining(CharSequence arg0, CharSequence arg1, CharSequence arg2) { throw new RuntimeException("skeleton method"); }
    public static <T, U, A, R> Collector<T,?,R> mapping(Function<? super T,? extends U> arg0, Collector<? super U,A,R> arg1) { throw new RuntimeException("skeleton method"); }
    public static <T, A, R, RR> Collector<T,A,RR> collectingAndThen(Collector<T,A,R> arg0, Function<R,RR> arg1) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Long> counting() { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Optional<T>> minBy(Comparator<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Optional<T>> maxBy(Comparator<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Integer> summingInt(ToIntFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Long> summingLong(ToLongFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Double> summingDouble(ToDoubleFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Double> averagingInt(ToIntFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Double> averagingLong(ToLongFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Double> averagingDouble(ToDoubleFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,T> reducing(T arg0, BinaryOperator<T> arg1) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Optional<T>> reducing(BinaryOperator<T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T, U> Collector<T,?,U> reducing(U arg0, Function<? super T,? extends U> arg1, BinaryOperator<U> arg2) { throw new RuntimeException("skeleton method"); }
    public static <T, K> Collector<T,?,Map<K,List<T>>> groupingBy(Function<? super T,? extends K> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T, K, A, D> Collector<T,?,Map<K,D>> groupingBy(Function<? super T,? extends K> arg0, Collector<? super T,A,D> arg1) { throw new RuntimeException("skeleton method"); }
    public static <T, K, D, A, M> Collector<T,?,M> groupingBy(Function<? super T,? extends K> arg0, Supplier<M> arg1, Collector<? super T,A,D> arg2) { throw new RuntimeException("skeleton method"); }
    public static <T, K> Collector<T,?,ConcurrentMap<K,List<T>>> groupingByConcurrent(Function<? super T,? extends K> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T, K, A, D> Collector<T,?,ConcurrentMap<K,D>> groupingByConcurrent(Function<? super T,? extends K> arg0, Collector<? super T,A,D> arg1) { throw new RuntimeException("skeleton method"); }
    public static <T, K, A, D, M> Collector<T,?,M> groupingByConcurrent(Function<? super T,? extends K> arg0, Supplier<M> arg1, Collector<? super T,A,D> arg2) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,Map<Boolean,List<T>>> partitioningBy(Predicate<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T, D, A> Collector<T,?,Map<Boolean,D>> partitioningBy(Predicate<? super T> arg0, Collector<? super T,A,D> arg1) { throw new RuntimeException("skeleton method"); }
    public static <T, K, U> Collector<T,?,Map<K,U>> toMap(Function<? super T,? extends K> arg0, Function<? super T,? extends U> arg1) { throw new RuntimeException("skeleton method"); }
    public static <T, K, U> Collector<T,?,Map<K,U>> toMap(Function<? super T,? extends K> arg0, Function<? super T,? extends U> arg1, BinaryOperator<U> arg2) { throw new RuntimeException("skeleton method"); }
    public static <T, K, U, M> Collector<T,?,M> toMap(Function<? super T,? extends K> arg0, Function<? super T,? extends U> arg1, BinaryOperator<U> arg2, Supplier<M> arg3) { throw new RuntimeException("skeleton method"); }
    public static <T, K, U> Collector<T,?,ConcurrentMap<K,U>> toConcurrentMap(Function<? super T,? extends K> arg0, Function<? super T,? extends U> arg1) { throw new RuntimeException("skeleton method"); }
    public static <T, K, U> Collector<T,?,ConcurrentMap<K,U>> toConcurrentMap(Function<? super T,? extends K> arg0, Function<? super T,? extends U> arg1, BinaryOperator<U> arg2) { throw new RuntimeException("skeleton method"); }
    public static <T, K, U, M> Collector<T,?,M> toConcurrentMap(Function<? super T,? extends K> arg0, Function<? super T,? extends U> arg1, BinaryOperator<U> arg2, Supplier<M> arg3) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,IntSummaryStatistics> summarizingInt(ToIntFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,LongSummaryStatistics> summarizingLong(ToLongFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> Collector<T,?,DoubleSummaryStatistics> summarizingDouble(ToDoubleFunction<? super T> arg0) { throw new RuntimeException("skeleton method"); }
}
