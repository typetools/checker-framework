import org.checkerframework.checker.nullness.qual.Nullable;

class Issue6804 {
  @FunctionalInterface
  interface Func<T> {
    T get();
  }

  void method1(Func<@Nullable Integer> y, String[] array) {
    // :: error: (unboxing.of.nullable)
    String x = array[y.get()];
  }

  void method2(String[] array, @Nullable Integer i) {
    // :: error: (unboxing.of.nullable)
    String z = array[i];
  }

  @SuppressWarnings("new.array")
  void method3(@Nullable Integer i, @Nullable Integer j) {
    String[][][] array =
        new String[0][0]
            // :: error: (unboxing.of.nullable)
            [i];
    // :: error: (unboxing.of.nullable)
    String[] array2 = new String[j];
  }
}
