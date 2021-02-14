// Test case for Issue 1039:
// https://github.com/typetools/checker-framework/issues/1039

public class Issue1039<T extends Issue1039<T>> {
    Issue1039<?> foo() {
        return new Issue1039<>();
    }
}
