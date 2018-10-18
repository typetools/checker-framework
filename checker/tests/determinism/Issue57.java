import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue57<T extends @PolyDet("use") Object> implements @NonDet Iterator<T> {
    public Issue57(@PolyDet List<T> a) {}

    public Issue57() {}

    public @PolyDet("down") boolean hasNext() {
        return false;
    }

    public T next() {
        return null;
    }
}
