package java.util.function;

public interface DoubleUnaryOperator {
    double applyAsDouble(double arg0);
    DoubleUnaryOperator compose(DoubleUnaryOperator arg0);
    DoubleUnaryOperator andThen(DoubleUnaryOperator arg0);
    static DoubleUnaryOperator identity() { throw new RuntimeException("skeleton method"); }
}
