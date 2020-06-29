import org.checkerframework.checker.nullness.qual.Nullable;

class ArrayIndex {
    void foo(@Nullable Object[] a, int i) {
        if (a[i] != null) {
            a[i].hashCode();
        }
        if (a[i + 1] != null) {
            a[i + 1].hashCode();
        }
        if (a[i + 1] != null) {
            // :: error: (dereference.of.nullable)
            a[i].hashCode();
        }
    }
}
