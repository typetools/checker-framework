// Passing a resource to an @Owning or @MustCallAlias parameter of a superclass constructor
// transfers ownership, also when the class being instantiated is anonymous.

import java.io.Closeable;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.checkerframework.checker.calledmethods.qual.EnsuresCalledMethods;
import org.checkerframework.checker.mustcall.qual.CreatesMustCallFor;
import org.checkerframework.checker.mustcall.qual.InheritableMustCall;
import org.checkerframework.checker.mustcall.qual.Owning;
import org.checkerframework.checker.nullness.qual.Nullable;

public class AnonymousClassOwningParameter {
  class Inner {
    // :: error: (required.method.not.called)
    Inner(@Owning Closeable c) {}
  }

  class InnerTwo {
    // :: error: (required.method.not.called)
    InnerTwo(Closeable notOwned, @Owning Closeable owned) {}
  }

  class InnerInt {
    InnerInt(int i) {}
  }

  class InnerVarargs {
    InnerVarargs(Closeable... cs) {}
  }

  class InnerOwningAndVarargs {
    // :: error: (required.method.not.called)
    InnerOwningAndVarargs(@Owning Closeable owned, Closeable... rest) {}
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

  void anonymousExplicitThis(@Owning Closeable c) {
    this.new Inner(c) {};
  }

  void anonymousExplicitEnclosingTwo(
      AnonymousClassOwningParameter outer,
      // :: error: (required.method.not.called)
      @Owning Closeable notOwned,
      @Owning Closeable owned) {
    outer.new InnerTwo(notOwned, owned) {};
  }

  void anonymousExplicitEnclosingInt(AnonymousClassOwningParameter outer) {
    outer.new InnerInt(5) {};
  }

  void anonymousExplicitEnclosingNoVarargs(AnonymousClassOwningParameter outer) {
    outer.new InnerVarargs() {};
  }

  void anonymousExplicitEnclosingVarargs(
      AnonymousClassOwningParameter outer, Closeable a, Closeable b) {
    outer.new InnerVarargs(a, b) {};
  }

  void anonymousExplicitEnclosingOwningAndVarargs(
      AnonymousClassOwningParameter outer, @Owning Closeable owned, Closeable rest) {
    outer.new InnerOwningAndVarargs(owned, rest) {};
  }

  void anonymousNested(@Owning Closeable c) {
    new Nested(c) {};
  }

  void anonymousMustCallAlias(@Owning InputStream in) throws IOException {
    InputStream s = new FilterInputStream(in) {};
    s.close();
  }

  @InheritableMustCall("disconnect")
  static class Holder {

    private @Owning @Nullable Node head = null;

    @CreatesMustCallFor("this")
    void addNamed() {
      head = new Node(head);
    }

    @CreatesMustCallFor("this")
    void addAnonymous() {
      head = new Node(head) {};
    }

    @EnsuresCalledMethods(value = "this.head", methods = "disconnect")
    void disconnect() {
      if (head != null) {
        head.disconnect();
      }
    }
  }

  @InheritableMustCall("disconnect")
  static class Node {
    private final @Owning @Nullable Node next;

    Node(@Owning @Nullable Node next) {
      this.next = next;
    }

    @EnsuresCalledMethods(value = "this.next", methods = "disconnect")
    void disconnect() {
      if (next != null) {
        next.disconnect();
      }
    }
  }
}
