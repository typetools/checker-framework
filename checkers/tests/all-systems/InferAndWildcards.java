import checkers.nullness.quals.*;
import checkers.javari.quals.*;

@SuppressWarnings({"interning", "oigj"})
class InferAndWildcards {
    <UUU extends @Nullable @ReadOnly Object> @Nullable Class<? extends UUU> b(Class<UUU> clazz) {
        return clazz;
    }

    <TTT> void a(Class<TTT> clazz) {
        Class<? extends TTT> v = b(clazz);
    }
}
