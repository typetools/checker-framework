package java.util.function;

public interface DoubleConsumer {
    void accept(double value);
    default DoubleConsumer andThen(DoubleConsumer after) { throw new RuntimeException("skeleton method"); }
}
