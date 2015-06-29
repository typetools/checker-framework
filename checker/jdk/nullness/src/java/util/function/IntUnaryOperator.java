package java.util.function;

public interface IntUnaryOperator {
    int applyAsInt(int arg0);
    IntUnaryOperator compose(IntUnaryOperator arg0);
    IntUnaryOperator andThen(IntUnaryOperator arg0);
    static IntUnaryOperator identity() { throw new RuntimeException("skeleton method"); }
}
