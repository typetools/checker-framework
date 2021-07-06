import org.checkerframework.checker.nullness.qual.Nullable;

public class VarargsNullness2 {

  public static void method1(Object... args) {}

  public static void method2(Object @Nullable ... args) {}

  public static void main(String[] args) {
    // :: error: (varargs)
    // :: error: (argument)
    // :: warning: non-varargs call of varargs method with inexact argument type for last parameter;
    method1(null);
    // :: warning: non-varargs call of varargs method with inexact argument type for last parameter;
    method2(null);
  }
}
