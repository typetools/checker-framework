package java.util.function;

@FunctionalInterface
public interface LongConsumer {
    void accept(long value);
    default LongConsumer andThen(LongConsumer after) { throw new RuntimeException("skeleton method"); }
}
