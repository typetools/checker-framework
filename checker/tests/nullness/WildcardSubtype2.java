import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WildcardSubtype2 {
  class MyClass {}

  class Visitor<T, S> {
    String visit(T p) {
      return "";
    }
  }

  class MyClassVisitor extends Visitor<@Nullable MyClass, @Nullable MyClass> {}

  class NonNullMyClassVisitor extends Visitor<@NonNull MyClass, @NonNull MyClass> {}

  void test(MyClassVisitor myClassVisitor, NonNullMyClassVisitor nonNullMyClassVisitor) {
    // :: error: (argument.type.incompatible)
    take(new Visitor<@Nullable Object, @Nullable Object>());
    // :: error: (argument.type.incompatible)
    take(new Visitor<@Nullable Object, @Nullable Object>());
    Visitor<?, ?> visitor1 = myClassVisitor;
    Visitor<?, ?> visitor2 = nonNullMyClassVisitor;

    // :: error: (assignment.type.incompatible)
    Visitor<? extends @NonNull Object, ? extends @NonNull Object> visitor3 = myClassVisitor;
    Visitor<? extends @NonNull Object, ? extends @NonNull Object> visitor4 = nonNullMyClassVisitor;

    Visitor<? extends @NonNull Object, ? extends @NonNull Object> visitor5 =
        // :: error: (assignment.type.incompatible)
        new MyClassVisitor();
    Visitor<? extends @NonNull Object, ? extends @NonNull Object> visitor6 =
        // :: error: (assignment.type.incompatible)
        new MyClassVisitor();
    // :: error: (argument.type.incompatible)
    take(new MyClassVisitor());
    // :: error: (argument.type.incompatible)
    take(new MyClassVisitor());
  }

  void take(Visitor<@NonNull ? extends @NonNull Object, @NonNull ? extends @NonNull Object> v) {}

  void bar() {
    // :: error: (argument.type.incompatible)
    take(new Visitor<@Nullable Object, @Nullable Object>());
    // :: error: (argument.type.incompatible)
    take(new MyClassVisitor());
  }

  void baz() {
    // :: error: (argument.type.incompatible)
    take(new MyClassVisitor());
    take(new NonNullMyClassVisitor());
  }
}
