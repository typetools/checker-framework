import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

// various tests for the @Pure annotation
public class Purity {

  String f1, f2, f3;
  String[] a;

  // class with a (potentially) non-pure constructor
  private static class NonPureClass {}

  // class with a pure constructor
  private static class PureClass {
    @Pure
    // :: warning: (purity.deterministic.constructor)
    public PureClass() {}
  }

  // class with a side-effect-free constructor
  private static class SEClass {
    @SideEffectFree
    public SEClass() {}
  }

  // a method that is not pure (no annotation)
  void nonpure() {}

  @Pure
  String pure() {
    return "";
  }

  @Pure
  // :: warning: (purity.deterministic.void.method)
  void t1() {}

  @SideEffectFree
  void t1b() {}

  @Deterministic
  // :: warning: (purity.deterministic.void.method)
  void t1c() {}

  @Pure
  String t2() {
    return "";
  }

  @Pure
  String t3() {
    // :: error: (purity.not.deterministic.not.sideeffectfree.call)
    nonpure();
    // :: error: (purity.not.deterministic.call)
    t16b(); // Calling a @SideEffectFree method
    // :: error: (purity.not.sideeffectfree.call)
    t16c(); // Calling a @Deterministic method
    return "";
  }

  @Pure
  String t4() {
    pure();
    return "";
  }

  @Pure
  int t5() {
    int i = 1;
    return i;
  }

  @Pure
  int t6() {
    int j = 0;
    for (int i = 0; i < 10; i++) {
      j = j - i;
    }
    return j;
  }

  @Pure
  String t7() {
    if (true) {
      return "a";
    }
    return "";
  }

  @Pure
  int t8() {
    return 1 - 2 / 3 * 2 % 2;
  }

  @Pure
  String t9() {
    return "b" + "a";
  }

  @Pure
  String t10() {
    // :: error: (purity.not.deterministic.not.sideeffectfree.assign.field)
    f1 = "";
    // :: error: (purity.not.deterministic.not.sideeffectfree.assign.field)
    f2 = "";
    return "";
  }

  @Pure
  String t11(Purity l) {
    // :: error: (purity.not.deterministic.not.sideeffectfree.assign.array)
    l.a[0] = "";
    return "";
  }

  @Pure
  String t12(String[] s) {
    // :: error: (purity.not.deterministic.not.sideeffectfree.assign.array)
    s[0] = "";
    return "";
  }

  @Pure
  String t13() {
    // :: error: (purity.not.deterministic.object.creation)
    PureClass p = new PureClass();
    return "";
  }

  @SideEffectFree
  String t13b() {
    PureClass p = new PureClass();
    return "";
  }

  @SideEffectFree
  String t13d() {
    SEClass p = new SEClass();
    return "";
  }

  @Deterministic
  String t13c() {
    // :: error: (purity.not.deterministic.object.creation)
    PureClass p = new PureClass();
    return "";
  }

  @Pure
  String t14() {
    String i = "";
    i = "a";
    return i;
  }

  @Pure
  String t15() {
    String[] s = new String[1];
    return s[0];
  }

  @Pure
  String t16() {
    try {
      int i = 1 / 0;
      // :: error: (purity.not.deterministic.catch)
    } catch (Throwable t) {
      // ...
    }
    return "";
  }

  @SideEffectFree
  String t16b() {
    try {
      int i = 1 / 0;
    } catch (Throwable t) {
      // ...
    }
    return "";
  }

  @Deterministic
  String t16c() {
    try {
      int i = 1 / 0;
      // :: error: (purity.not.deterministic.catch)
    } catch (Throwable t) {
      // ...
    }
    return "";
  }

  @Pure
  String t12() {
    // :: error: (purity.not.sideeffectfree.call)
    // :: error: (purity.not.deterministic.object.creation)
    NonPureClass p = new NonPureClass();
    return "";
  }

  @Deterministic
  String t17a(Purity l) {
    // :: error: (purity.not.deterministic.assign.field)
    f1 = "";
    // :: error: (purity.not.deterministic.assign.array)
    l.a[0] = "";
    // :: error: (purity.not.deterministic.call)
    nonpure();
    // :: error: (purity.not.deterministic.call)
    return t16b(); // Calling a @SideEffectFree method
  }

  @SideEffectFree
  String t17b() {
    // :: error: (purity.not.sideeffectfree.assign.field)
    f1 = "";
    // :: error: (purity.not.sideeffectfree.call)
    NonPureClass p = new NonPureClass();
    // :: error: (purity.not.sideeffectfree.call)
    nonpure();
    // :: error: (purity.not.sideeffectfree.call)
    return t16c(); // Calling a @Deterministic method
  }

  // @Pure annotations on the overridden implementation.
  class Super {
    @Pure
    int m1(int arg) {
      return 0;
    }

    @Pure
    int m2(int arg) {
      return 0;
    }

    int m3(int arg) {
      return 0;
    }

    int m4(int arg) {
      return 0;
    }
  }

  class Sub extends Super {
    @Pure
    int m1(int arg) {
      return 0;
    }

    int m2(int arg) {
      return 0;
    }

    @Pure
    int m3(int arg) {
      return 0;
    }

    int m4(int arg) {
      return 0;
    }
  }

  class MyClass extends Object {
    public int hashCode() {
      return 42;
    }
  }
}
