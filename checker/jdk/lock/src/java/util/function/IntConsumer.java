package java.util.function;

public interface IntConsumer {
    void accept(int value);
    default IntConsumer andThen(IntConsumer after) { throw new RuntimeException("skeleton method"); }
}
