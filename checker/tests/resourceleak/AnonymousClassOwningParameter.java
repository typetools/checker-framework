// Passing a resource to an @Owning parameter of a superclass constructor transfers ownership, also
// when the class being instantiated is anonymous.

import java.io.Closeable;
import org.checkerframework.checker.mustcall.qual.Owning;

public class AnonymousClassOwningParameter {
  class Inner {
    // :: error: (required.method.not.called)
    Inner(@Owning Closeable c) {}
  }

  class InnerTwo {
    // :: error: (required.method.not.called)
    InnerTwo(Closeable notOwned, @Owning Closeable owned) {}
  }

  static class Nested {
    // :: error: (required.method.not.called)
    Nested(@Owning Closeable c) {}
  }

  void named(AnonymousClassOwningParameter outer, @Owning Closeable c) {
    outer.new Inner(c);
  }

  void anonymousImplicitEnclosing(@Owning Closeable c) {
    new Inner(c) {};
  }

  void anonymousExplicitEnclosing(AnonymousClassOwningParameter outer, @Owning Closeable c) {
    outer.new Inner(c) {};
  }

  void anonymousExplicitEnclosingTwo(
      AnonymousClassOwningParameter outer,
      // :: error: (required.method.not.called)
      @Owning Closeable notOwned,
      @Owning Closeable owned) {
    outer.new InnerTwo(notOwned, owned) {};
  }

  void anonymousNested(@Owning Closeable c) {
    new Nested(c) {};
  }
}
