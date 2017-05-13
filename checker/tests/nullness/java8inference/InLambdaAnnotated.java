// @skip-test until fixed
// @below-java8-jdk-skip-test

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
                Boxes.transform(
                        el -> {
                            return Mine.some();
                        });

        Function<String, Mine<@Nullable Integer>> fun;

        void bar() {
            Box<Mine<@Nullable Integer>> h = Boxes.transform(fun);
        }
    }
}
