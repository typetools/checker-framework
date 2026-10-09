// Test case for https://github.com/typetools/checker-framework/issues/8238
// framework/tests/all-systems/Issue8238.java tests that the crash is gone; this test checks the
// qualifiers of the type argument that is inferred from the captured wildcard.

import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class Issue8238 {

  interface Fn<A, B> {
    B apply(A a);
  }

  static class Tag<T> {}

  static class Box<K, V> {
    V compute(K key, Fn<? super K, ? extends V> fn) {
      throw new UnsupportedOperationException();
    }
  }

  <T> T create(Tag<T> tag) {
    throw new UnsupportedOperationException();
  }

  void takesUntainted(Fn<Tag<? extends @Untainted String>, @Untainted String> f) {}

  void takesTainted(Fn<Tag<? extends @Tainted String>, @Untainted String> f) {}

  // The wildcard is captured, so T is inferred to be the capture of `? extends @Untainted String`,
  // whose upper bound is @Untainted String.
  void untaintedWildcard() {
    takesUntainted(this::create);
  }

  // Here the capture's upper bound is @Tainted String, which is not a subtype of the function
  // type's @Untainted String return type.
  void taintedWildcard() {
    // :: error: (type.arguments.not.inferred)
    takesTainted(this::create);
  }

  // The wildcard comes from the receiver's type arguments rather than from the call's arguments,
  // as in the original issue.
  void fromReceiver(
      Box<Tag<? extends @Untainted String>, @Untainted String> box,
      Tag<? extends @Untainted String> tag) {
    box.compute(tag, this::create);
  }
}
