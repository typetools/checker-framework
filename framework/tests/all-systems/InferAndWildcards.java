@SuppressWarnings("interning")
class InferAndWildcards {
    <UUU> Class<? extends UUU> b(Class<UUU> clazz) {
        return clazz;
    }
    @SuppressWarnings("determinism")
    <TTT> void a(Class<TTT> clazz) {
        Class<? extends TTT> v = b(clazz);
    }
}
