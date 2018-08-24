import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
public class Issue22<T extends Comparable<T>> {
    public int compare(T[] a1, T[] a2) {
        return a1[0].compareTo(a2[0]);
    }
}
