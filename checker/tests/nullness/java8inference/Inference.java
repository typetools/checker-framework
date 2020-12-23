// Test case for Issue 979:
// https://github.com/typetools/checker-framework/issues/979

class MyStream<T> {
    @SuppressWarnings("nullness")
    <R, A> R collect(MyCollector<? super T, A, R> collector) {
        return null;
    }
}

interface MyCollector<T, A, R> {}

public class Inference {

    @SuppressWarnings("nullness")
    static <E> MyCollector<E, ?, MyStream<E>> toImmutableStream() {
        return null;
    }

    MyStream<String> test(MyStream<String> p) {
        /* Need Java 8 assignment context to correctly infer type arguments.
             return p.collect(toImmutableStream());
                        ^
           found   : @Initialized @NonNull MyStream<? extends @Initialized @Nullable Object>
           required: @Initialized @NonNull MyStream<@Initialized @NonNull String>
        */
        return p.collect(toImmutableStream());
    }
}
