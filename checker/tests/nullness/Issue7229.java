import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue7229 {
  void f(@Nullable Integer[] array) {
    // :: error: (unboxing.of.nullable)
    int x = array[0];
  }
}
