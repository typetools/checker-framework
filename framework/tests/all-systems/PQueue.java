public class PQueue<P> {
    public static <E> PQueue<E> create(Iterable<? extends E> p, Builder<E> b) {
        return b.create(p);
    }

    public static final class Builder<B> {
        public <T extends B> PQueue<T> create(Iterable<? extends T> p) {
            throw new RuntimeException();
        }
    }
}
