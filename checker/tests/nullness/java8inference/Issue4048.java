// Test case for issue #4048: https://tinyurl.com/cfissue/4048

// @skip-test until the issue is fixed

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

abstract class Issue4048 {
    @Nullable Number m1(List<? extends Number> numbers) {
        return getOnlyElement1(numbers);
    }

    abstract <T> @Nullable T getOnlyElement1(Iterable<T> values);

    @Nullable Number m2(List<? extends Number> numbers) {
        return getOnlyElement2(numbers);
    }

    abstract <T> @Nullable T getOnlyElement2(Iterable<? extends T> values);
}
