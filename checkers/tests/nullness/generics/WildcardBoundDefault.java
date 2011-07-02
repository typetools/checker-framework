import checkers.nullness.quals.*;
import checkers.quals.*;

class MyClass<T extends @Nullable Object> {}

@DefaultQualifier(value="Nullable", locations=DefaultLocation.UPPER_BOUNDS)
class Varargs {
  void test() { ignore(newInstance()); }
  static void ignore(MyClass<?>... consumer) {}
  static <T> MyClass<T> newInstance() { return new MyClass<T>(); }
}
