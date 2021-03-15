// Test case for Issue #1049
// https://github.com/typetools/checker-framework/issues/1049
public class Issue1049 {
    interface Gen<T extends Gen<T>> {
        T get();
    }

    @SuppressWarnings("nulltest.redundant")
    void bar(Gen<?> g) {
        Gen<?> l = g.get() != null ? g.get() : g;
    }
}
