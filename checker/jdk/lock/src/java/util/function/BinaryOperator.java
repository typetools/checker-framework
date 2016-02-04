package java.util.function;

import java.util.Comparator;

public interface BinaryOperator<T> extends BiFunction<T,T,T> {
    public static <T> BinaryOperator<T> minBy(Comparator<? super T> arg0) { throw new RuntimeException("skeleton method"); }
    public static <T> BinaryOperator<T> maxBy(Comparator<? super T> arg0) { throw new RuntimeException("skeleton method"); }
}
