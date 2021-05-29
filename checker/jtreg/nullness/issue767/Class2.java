import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Class2 {
  public static @Nullable Object field;
  public @Nullable Object instanceField;

  public static void method(Class1 class1) {
    Class1.method();
    @NonNull Object o = Class1.field;
    Class1.method2();
    @NonNull Object o2 = field;

    class1.instanceMethod();
    @NonNull Object o3 = class1.instanceField;
  }

  void test() {
    Class1.method3(this);
    @NonNull Object o = instanceField;
  }
}
