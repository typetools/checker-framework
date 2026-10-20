// Test case for https://github.com/typetools/checker-framework/issues/8300

public class Issue8300 {
  static class Box<T> {}

  static <T> Box<? super T> superBox(Box<T> b) {
    throw new RuntimeException();
  }

  static <X> X takeSuper(Box<? super X> x) {
    throw new RuntimeException();
  }

  static void noTarget(Box<String> b) {
    takeSuper(superBox(b));
  }

  static void target(Box<String> b) {
    String s = takeSuper(superBox(b));
  }

  static <T> Box<? super T> superBoxNoArg() {
    throw new RuntimeException();
  }

  static <T> Box<? super T> superBoxOf(T t) {
    throw new RuntimeException();
  }

  static void unconstrained() {
    String s = takeSuper(superBoxNoArg());
  }

  static void nullArgument() {
    String s = takeSuper(superBoxOf(null));
  }

  static void sameType(Integer i) {
    Integer s = takeSuper(superBoxOf(i));
  }

  // The glb of the capture of `? super Integer` and Number is Integer.
  static void supertypeTarget(Integer i) {
    Number s = takeSuper(superBoxOf(i));
  }

  static class BoundedBox<T extends CharSequence> {}

  static <T extends CharSequence> BoundedBox<? super T> superBoundedBox(BoundedBox<T> b) {
    throw new RuntimeException();
  }

  static <X extends CharSequence> X takeSuperBounded(BoundedBox<? super X> x) {
    throw new RuntimeException();
  }

  static void targetBounded(BoundedBox<String> b) {
    String s = takeSuperBounded(superBoundedBox(b));
  }
}
