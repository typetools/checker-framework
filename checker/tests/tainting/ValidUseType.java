// Test case for Issue 2247
// https://github.com/typetools/checker-framework/issues/2247

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

@NonNull class ValidUseType {
    void test(@Nullable ValidUseType object) {}
}
