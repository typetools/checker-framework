import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.Covariant;

public class CovariantError {

  @Covariant(0)
  static class MyClass<X> {}

  <T> void cls(MyClass<T> p1, T p2) {}

  void use(CovariantError t) {
    // TODO: This is a false positive.
    // :: error: [type.arguments.not.inferred]
    cls(this.getMyClass(), t);
  }

  MyClass<@Untainted CovariantError> getMyClass() {
    throw new RuntimeException();
  }
}
