import org.checkerframework.checker.nullness.qual.*;

class ArrayInitBug {

    @Nullable Object @Nullable [] aa;

    public ArrayInitBug() {
        aa = null;
    }
}
