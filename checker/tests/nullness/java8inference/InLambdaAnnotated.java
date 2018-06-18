import org.checkerframework.checker.nullness.qual.Nullable;

class InLambdaAnnotated {
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
        // The nested Mine.some() needs to infer the right type.
        Box<Mine<@Nullable Integer>> g =
                // TODO: This is a false positive.
                // :: error: (assignment.type.incompatible)
                Boxes.transform(
                        el -> {
                            return Mine.some();
                        });

        void bar(Function<String, Mine<@Nullable Integer>> fun) {
            Box<Mine<@Nullable Integer>> h = Boxes.transform(fun);
        }
    }
}
