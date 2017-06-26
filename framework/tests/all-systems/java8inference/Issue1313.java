// Test case for Issue 1313.
// https://github.com/typetools/checker-framework/issues/1313
// @below-java8-jdk-skip-test

import java.util.stream.Collector;
import java.util.stream.Stream;

interface MyList1313<E> extends Iterable<E> {}

@SuppressWarnings("") // check for crashes
class Issue1313 {
    Stream<?> s;
    Iterable<?> i = s.collect(toMyList1313());

    <E> Collector<E, ?, MyList1313<E>> toMyList1313() {
        return null;
    }
}
