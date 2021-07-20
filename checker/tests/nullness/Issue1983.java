// Test case for Issue 1983:
// https://github.com/typetools/checker-framework/issues/1983

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.function.Function;

public class Issue1983 {

    @SuppressWarnings("initialization.field.uninitialized")
    Converter<String> converter;

    void test(List<Object[]> params) {
        func1(transform(params, p -> of(converter.as((String) p[0]))));
    }

    static class Converter<T> {

        @SuppressWarnings("nullness")
        public Converter<T> as(@Nullable T value) {
            return null;
        }
    }

    @SuppressWarnings("nullness")
    static <T> List<T> of(T t) {
        return null;
    }

    @SuppressWarnings("nullness")
    <V> V func1(List<List<Converter<?>>> bulkParameterValues) {
        return null;
    }

    @SuppressWarnings("nullness")
    static <F, T> List<T> transform(List<F> fromList, Function<? super F, ? extends T> function) {
        return null;
    }
}
