// Test case for https://github.com/typetools/checker-framework/issues/6832

import java.util.Collection;
import java.util.LinkedList;
import java.util.function.Supplier;
import org.checkerframework.checker.nullness.qual.Nullable;

public class Issue6832 {

  static class A {
    public <T> @Nullable T m(T x) {
      return null;
    }
  }

  public static <T> void invokeAny(Collection<Supplier<T>> p) {
    p.toString();
  }

  public static void main(String[] args) {
    A x = new A();
    // :: error: (argument)
    invokeAny(x.m(new LinkedList<Supplier<String>>()));
  }
}
