// Test case for Issue 1705
// https://github.com/typetools/checker-framework/issues/1705

import java.util.function.Function;
import org.checkerframework.checker.tainting.qual.PolyTainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue1705 {
    static class MySecondClass<X> {
        @PolyTainted MySecondClass<X> doOnComplete(@PolyTainted MySecondClass<X> this) {
            throw new RuntimeException();
        }
    }

    <R> @PolyTainted R to(@PolyTainted Issue1705 this, Function<? super Issue1705, R> arg0) {
        throw new RuntimeException();
    }

    static <T> Function<? super T, MySecondClass<T>> empty() {
        throw new RuntimeException();
    }

    void test(@Untainted Issue1705 a) {
        @Untainted Object z = a.to(empty()).doOnComplete();
    }
}
