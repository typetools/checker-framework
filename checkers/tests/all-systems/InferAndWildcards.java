import checkers.nullness.quals.*;

@SuppressWarnings({"interning", "oigj"})
class InferAndWildcards {
    <UUU extends @Nullable Object> @Nullable Class<? extends UUU> b(Class<UUU> clazz) {
        return clazz;
    }

    <TTT> void a(Class<TTT> clazz) {
        Class<? extends TTT> v = b(clazz);
    }
}
