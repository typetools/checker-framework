// This class is not compiled with the Nullness Checker,
// so that no annotations are stored in bytecode.
public class Box<T> {
    static <S> Box<S> of(S in) {
        // Implementation doesn't matter.
        return null;
    }

    static void consume(Box<?> producer) {}
}
