// Test case for Issue 1809:
// https://github.com/typetools/checker-framework/issues/1809

// Note that -AatfCacheSize=5 is required to exercise the problem.
// This test is to ensure the basic code compiles.
// For a reproduction of the issue, see checker/jtreg/nullness/Issue1809.java

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
abstract class Issue1809 {

    abstract <T> Stream<T> concat(Stream<? extends T>... streams);

    abstract Optional<A> f();

    private static class A {}

    interface B {
        List<C> g();
    }

    interface C {
        List<S> h();
    }

    interface S {}

    private Stream<A> xrefsFor(B b) {
        return concat(b.g().stream().flatMap(a -> a.h().stream().map(c -> f())))
                .filter(Optional::isPresent)
                .map(Optional::get);
    }
}
