public class InLambda {
    static class Mine<T> {
        @SuppressWarnings("nullness") // just a utility
        static <S> Mine<S> some() {
            return null;
        }
    }

    interface Function<T, R> {
        R apply(T t);
    }

    interface Box<V> {}

    static class Boxes {
        @SuppressWarnings("nullness") // just a utility
        static <O> Box<O> transform(Function<String, ? extends O> function) {
            return null;
        }
    }

    class Infer {
        Box<Mine<Integer>> f =
                Boxes.transform(
                        el -> {
                            return Mine.some();
                        });
    }
}
