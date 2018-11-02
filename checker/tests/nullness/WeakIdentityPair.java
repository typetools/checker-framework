import java.lang.ref.WeakReference;
import org.checkerframework.checker.nullness.qual.*;

public class WeakIdentityPair<T1 extends Object> {

    private final WeakReference<T1> a;

    public WeakIdentityPair(T1 a) {
        this.a = new WeakReference<>(a);
    }

    public @Nullable T1 getA() {
        return a.get();
    }
}
