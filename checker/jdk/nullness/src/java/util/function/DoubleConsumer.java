package java.util.function;

@FunctionalInterface
public interface DoubleConsumer {
    void accept(double value);
    default DoubleConsumer andThen(DoubleConsumer after) { throw new RuntimeException("skeleton method"); }
}
