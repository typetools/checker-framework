import tests.util.*;

// Test case for Issue 131:
// http://code.google.com/p/checker-framework/issues/detail?id=131
public class GenericTest1 {
  public interface Foo<T> {}
  public interface Bar<T, C, E extends Foo<C>> extends Foo<T> {}

  public <T> void test(Foo<T> foo) {
    Bar<?, ?, ?> bar = foo instanceof Bar<?, ?, ?>
        ? (Bar<?, ?, ?>) foo : null;
  }
}
