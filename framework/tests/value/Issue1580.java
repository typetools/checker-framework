// Test case for Issue 1580
// https://github.com/typetools/checker-framework/issues/1580

public class Issue1580<R, K extends Issue1580<R, K>> {
    protected final Gen<R> field;

    protected Issue1580(K parent) {
        field = parent.field;
    }

    static class Gen<T> {}
}
