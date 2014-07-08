import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

class Super {
    Object f;
    Object g;

    @EnsuresNonNull("f")
    void setf() {
        f = new Object();
    }

    void setg() {
        g = null;
    }

}
