package java.util.function;

public interface LongUnaryOperator {
    long applyAsLong(long arg0);
    LongUnaryOperator compose(LongUnaryOperator arg0);
    LongUnaryOperator andThen(LongUnaryOperator arg0);
    static LongUnaryOperator identity() { throw new RuntimeException("skeleton method"); }
}
