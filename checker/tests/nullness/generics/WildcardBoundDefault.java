import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;

class MyClass<T extends @Nullable Object> {}

@DefaultQualifier(value=Nullable.class, locations=DefaultLocation.UPPER_BOUNDS)
class Varargs {
  void test() { ignore(newInstance()); }
  static void ignore(MyClass<?>... consumer) {}
  static <T> MyClass<T> newInstance() { return new MyClass<T>(); }
}
