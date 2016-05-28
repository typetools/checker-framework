import org.checkerframework.checker.nullness.qual.*;
import java.lang.ref.WeakReference;
class WeakRef {
    @PolyNull Object[] foo(WeakReference<@PolyNull Object[]> lookup) {
      return lookup.get();
    }

}
