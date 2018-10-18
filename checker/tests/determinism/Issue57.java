import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue57<T> implements @NonDet Iterator<T> {
    public Issue57() {}

    public @PolyDet("down") boolean hasNext() {
        return false;
    }

    public T next() {
        return null;
    }
}
