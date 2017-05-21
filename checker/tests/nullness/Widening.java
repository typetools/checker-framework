import org.checkerframework.checker.nullness.qual.Nullable;

class Widening {
    @Nullable Integer i;

    void inc(long amt) {}

    void foo() {
        inc(i == null ? 0 : i);
    }
}
