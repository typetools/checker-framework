import org.checkerframework.checker.nullness.qual.*;

/* Use @MonotonicNonNull as component type to ensure that null can never be
 * assigned into a component. Then, after a single iteration over the array,
 * we can be sure that all elements are non-null.
 * TODO: support for (i=0; i < a.length.... and change component type to non-null.
 */
public class ArrayLazyNN {
  void test1() {
    @MonotonicNonNull Object[] o1 = new @MonotonicNonNull Object[10];
    o1[0] = new Object();
    // :: error: (assignment.type.incompatible)
    o1[0] = null;
    // :: error: (assignment.type.incompatible)
    @NonNull Object[] o2 = o1;
    @SuppressWarnings("nullness")
    @NonNull Object[] o3 = o1;
  }
}
