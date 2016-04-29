package java.util.function;

public interface UnaryOperator<T> extends Function<T,T> {
    static <T> UnaryOperator<T> identity() { throw new RuntimeException("skeleton method"); }
}
