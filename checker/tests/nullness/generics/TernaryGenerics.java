import org.checkerframework.checker.nullness.qual.*;

public class TernaryGenerics {
  class Generic1<T extends @NonNull Object> {
    void cond(boolean b, T p) {
      // :: error: (assignment)
      @NonNull T r1 = b ? p : null;
      // :: error: (assignment)
      @NonNull T r2 = b ? null : p;
    }
  }

  class Generic2<T extends @Nullable Object> {
    void cond(boolean b, T p) {
      // :: error: (assignment)
      @NonNull T r1 = b ? p : null;
      // :: error: (assignment)
      @NonNull T r2 = b ? null : p;
    }
  }

  class Generic3<T> {
    void cond(boolean b, @Nullable T p) {
      @Nullable T r1 = b ? p : null;
      @Nullable T r2 = b ? null : p;
      // :: error: (assignment)
      @NonNull T r3 = b ? null : p;
    }
  }

  void array(boolean b) {
    String[] s = b ? new String[] {""} : null;
    // :: error: (dereference.of.nullable)
    s.toString();
  }

  void generic(boolean b, Generic1<String> p) {
    Generic1<String> s = b ? p : null;
    // :: error: (dereference.of.nullable)
    s.toString();
  }

  void primarray(boolean b) {
    long[] result = b ? null : new long[10];
    // :: error: (dereference.of.nullable)
    result.toString();
  }
}
