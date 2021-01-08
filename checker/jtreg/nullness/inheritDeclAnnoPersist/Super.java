import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.dataflow.qual.SideEffectFree;

public class Super {
    Object f;
    Object g;
    Object h;

    @EnsuresNonNull("f")
    void setf() {
        f = new Object();
    }

    void setg() {
        g = null;
    }

    @SideEffectFree
    void seth() {
        h = null;
    }
}
