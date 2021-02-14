@SuppressWarnings("interning")
public class InferAndWildcards {
    <UUU> Class<? extends UUU> b(Class<UUU> clazz) {
        return clazz;
    }

    <TTT> void a(Class<TTT> clazz) {
        Class<? extends TTT> v = b(clazz);
    }
}
