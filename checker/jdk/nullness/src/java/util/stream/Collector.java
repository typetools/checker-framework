package java.util.stream;

import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Collector<T, A, R> {
    Supplier<A> supplier();
    BiConsumer<A,T> accumulator();
    BinaryOperator<A> combiner();
    Function<A,R> finisher();
    Set<Characteristics> characteristics();

    static <T, R> Collector<T,R,R> of(Supplier<R> arg0, BiConsumer<R,T> arg1, BinaryOperator<R> arg2, Characteristics[] arg3)  { throw new RuntimeException("skeleton method"); }
    static <T, A, R> Collector<T,A,R> of(Supplier<A> arg0, BiConsumer<A,T> arg1, BinaryOperator<A> arg2, Function<A,R> arg3, Characteristics[] arg4) { throw new RuntimeException("skeleton method"); }

    enum Characteristics {CONCURRENT,
        UNORDERED,
        IDENTITY_FINISH
    }
}
