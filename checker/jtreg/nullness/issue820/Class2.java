import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Class2 {
  public static @Nullable Object field;
  public @Nullable Object instanceField;

  void test() {
    Class1.method3(this);
    @NonNull Object o = instanceField;
  }

  void test2() {
    //        instanceField = new Object(); Can't reproduce with assignment
    Class1.method3R(this);
  }
}
