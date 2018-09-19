import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class Issue22<T extends Comparable<T>> {
    public int compare(T[] a1, T[] a2) {
        // :: error: (argument.type.incompatible)
        return a1[0].compareTo(a2[0]);
    }
}
