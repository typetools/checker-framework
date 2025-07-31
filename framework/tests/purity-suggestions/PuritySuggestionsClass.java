import org.checkerframework.dataflow.qual.Deterministic;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;
import org.checkerframework.dataflow.qual.SideEffectsOnly;

// various tests for the checker to automatically suggest pure methods (most methods have been
// copied from Purity.java)

public class PuritySuggestionsClass {

  String f1, f2, f3;
  String[] a;
  static String staticString;

  // class with a (potentially) non-pure constructor
  private static class NonPureClass {
    String t;

    public NonPureClass() {
      staticString = "";
    }
  }

  // class with a pure constructor
  private static class PureClass {
    // :: warning: (purity.more.sideeffectfree)
    public PureClass() {}
  }

  // class with a pure constructor
  private static class PureClass2 {
    String t;

    // :: warning: (purity.more.sideeffectfree)
    public PureClass2() {
      t = "";
    }
  }

  // :: warning: (purity.more.sideeffectfree)
  void nonpure() {}

  @Pure
  String pure() {
    return "";
  }

  String t3() {
    nonpure();
    return "";
  }

  // :: warning: (purity.more.pure)
  String t4() {
    pure();
    return "";
  }

  // :: warning: (purity.more.pure)
  int t5() {
    int i = 1;
    return i;
  }

  // :: warning: (purity.more.pure)
  int t6() {
    int j = 0;
    for (int i = 0; i < 10; i++) {
      j = j - i;
    }
    return j;
  }

  // :: warning: (purity.more.pure)
  String t7() {
    if (true) {
      return "a";
    }
    return "";
  }

  // :: warning: (purity.more.pure)
  int t8() {
    return 1 - 2 / 3 * 2 % 2;
  }

  // :: warning: (purity.more.pure)
  String t9() {
    return "b" + "a";
  }

  String t10() {
    f1 = "";
    f2 = "";
    return "";
  }

  String t11(PuritySuggestionsClass l) {
    l.a[0] = "";
    return "";
  }

  String t12(String[] s) {
    s[0] = "";
    return "";
  }

  String t13() {
    PureClass p = new PureClass();
    return "";
  }

  // :: warning: (purity.more.pure)
  String t14() {
    String i = "";
    i = "a";
    return i;
  }

  // :: warning: (purity.more.pure)
  String t15() {
    String[] s = new String[1];
    return s[0];
  }

  // :: warning: (purity.more.sideeffectfree)
  String t16() {
    try {
      int i = 1 / 0;
    } catch (Throwable t) {
      // ...
    }
    return "";
  }

  // :: warning: (purity.more.sideeffectfree)
  String t16b() {
    try {
      int i = 1 / 0;
    } catch (Throwable t) {
      // ...
    }
    return "";
  }

  // :: warning: (purity.more.sideeffectfree)
  String t16c() {
    try {
      int i = 1 / 0;
    } catch (Throwable t) {
      // ...
    }
    return "";
  }

  // :: warning: (purity.more.pure)
  String t17() {
    return "";
  }

  @Deterministic
  // :: warning: (purity.more.sideeffectfree)
  String t18() {
    return "";
  }

  // :: warning: (purity.more.deterministic)
  String t19() {
    return t18();
  }

  String t12() {
    NonPureClass p = new NonPureClass();
    return "";
  }

  // Class with a method annotated with @SideEffectsOnly({})
  private static class EmptySideEffectsOnly {

    @SideEffectsOnly({})
    // :: warning: (purity.more.sideeffectfree)
    void foo() {}
  }

  @SideEffectFree
  @Deterministic
  // :: warning: (purity.effectively.pure)
  String shouldBeMarkedPure() {
    return "";
  }
}
