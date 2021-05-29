import org.checkerframework.checker.nullness.qual.*;

class SomeClass<@Nullable T> {
  T get() {
    throw new RuntimeException();
  }
}

public class AnnotatedTypeParams2 {

  void testPositive() {
    SomeClass<@Nullable String> l = new SomeClass<>();
    // :: error: (dereference.of.nullable)
    l.get().toString();
  }

  void testInvalidParam() {
    // :: error: (type.argument)
    SomeClass<@NonNull String> l;
  }
}
