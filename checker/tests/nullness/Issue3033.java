import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue3033 {

  class Test {

    void main() {
      Test obj1 = new Test();

      // No error as no explicit @Nullable or @NonNull annotation is given.
      if (obj1 instanceof Test) {
        Test obj2 = new Test();

        // :: error: (instanceof.nullable)
        if (obj1 instanceof @Nullable Test) {
          obj1 = null;
        }

        // :: warning: (instanceof.nonnull.redundant)
        if (obj2 instanceof @NonNull Test) {
          obj2 = obj1;
        }
      }
    }
  }
}
