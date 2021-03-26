import org.checkerframework.checker.nullness.qual.*;

class MyClass<@Nullable T> {
  T get() {
    throw new RuntimeException();
  }

  void testPositive() {
    MyClass<@Nullable String> l = new MyClass<>();
    // :: error: (dereference.of.nullable)
    l.get().toString();
  }

  void testInvalidParam() {
    // :: error: (type.argument.type.incompatible)
    MyClass<@NonNull String> l;
  }
}
