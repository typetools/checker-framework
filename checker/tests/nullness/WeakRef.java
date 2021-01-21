import java.lang.ref.WeakReference;
import org.checkerframework.checker.nullness.qual.*;

public class WeakRef {
    @PolyNull Object @Nullable [] foo(WeakReference<@PolyNull Object[]> lookup) {
        return lookup.get();
    }
}
