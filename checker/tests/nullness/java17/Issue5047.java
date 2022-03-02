// @below-java16-jdk-skip-test
// Test case for issue #5047: https://tinyurl.com/cfissue/5047

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Objects;

public class Issue5047 {}

class NumberParameterBuilder<T, U, V> {

    @Nullable Object minimum;
    @Nullable Object maximum;

    public boolean equals(final @Nullable Object o) {

        if (o instanceof NumberParameterBuilder<?, ?, ?> b) {
            return super.equals(o)
                    && Objects.equals(this.minimum, b.minimum)
                    && Objects.equals(this.maximum, b.maximum);
        } else {
            return false;
        }
    }
}
