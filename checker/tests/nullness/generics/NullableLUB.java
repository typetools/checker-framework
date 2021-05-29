import org.checkerframework.checker.nullness.qual.*;

/* Test case that illustrated an unsoundness in the unification of
 * type variables with non-type variables. The error did not previously
 * get raised, leading to a missed NPE.
 */
public class NullableLUB<T extends @Nullable Object> {
  // :: error: (initialization.field.uninitialized)
  T t;
  @Nullable T nt;

  T m(boolean b, T p) {
    T r1 = b ? p : null;
    nt = r1;
    // :: error: (assignment)
    t = r1;
    // :: error: (return)
    return r1;
  }

  public static void main(String[] args) {
    new NullableLUB<@NonNull Object>().m(false, new Object()).toString();
  }

  T m2(boolean b, T p) {
    T r1 = b ? null : p;
    nt = r1;
    // :: error: (assignment)
    t = r1;
    // :: error: (return)
    return r1;
  }
}
