// Test case for issue #979: https://github.com/typetools/checker-framework/issues/979

// @skip-test

import org.checkerframework.checker.nullness.qual.*;

public class Issue531 {
    public MyList<String> test(MyStream<String> stream) {
        return stream.collect(toList());
    }

    void foo(MyStream<String> stream) {}

    static <T> MyCollector<T, ?, MyList<T>> toList() {
        return new MyCollector<>();
    }
}

class MyList<T> {}

class MyCollector<T, A, R> {}

abstract class MyStream<T> {
    public abstract <R, A> R collect(MyCollector<? super T, A, R> c);
}
