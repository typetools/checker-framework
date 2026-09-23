// A method may inherit its `@SideEffectsOnly` annotation from a method that it overrides, rather
// than declare it.  Every use of the method -- a call, or a lambda that implements it -- is checked
// against the inherited annotation, just as it would be against a declared one.

import java.util.ArrayList;
import java.util.List;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

public class InheritedAnnotationAtUse {

  static int staticField;

  static class Super {
    List<String> f = new ArrayList<>();

    @SideEffectsOnly("this.f")
    void m() {
      f.add("x");
    }
  }

  static class Sub extends Super {
    // The overriding method inherits `@SideEffectsOnly("this.f")`.
    @Override
    void m() {
      f.add("y");
    }
  }

  @SideEffectsOnly("#1.f")
  void callsInheritedPermitted(Sub s) {
    s.m();
  }

  @SideEffectsOnly("this")
  void callsInheritedNotPermitted(Sub s) {
    // :: error: (purity.incorrect.sideeffectsonly)
    s.m();
  }

  // A lambda's body is checked against the annotation that the functional interface method
  // inherits.

  interface Base {
    @SideEffectsOnly("#1")
    void add(List<String> lst);
  }

  interface Derived extends Base {
    @Override
    void add(List<String> lst);
  }

  @SideEffectsOnly("this")
  void lambdaModifiesItsParameter() {
    // `#1` of `Base.add` is the lambda's own parameter.
    Derived d = lst -> lst.add("x");
  }

  @SideEffectsOnly("this")
  void lambdaModifiesAnotherList(List<String> other) {
    // :: error: (purity.incorrect.sideeffectsonly)
    Derived d = lst -> other.add("x");
  }

  @SideEffectsOnly("this")
  void lambdaModifiesStaticField() {
    // :: error: (purity.incorrect.sideeffectsonly)
    Derived d = lst -> staticField = 1;
  }
}
