// A test case that should not longer crash once issue #717 is fixed
// https://github.com/typetools/checker-framework/issues/717
public class Issue717 {

  public static <T extends Interface<? super T>> void foo2(T a, T b) {
    a.compareTo(b);
  }

  public static <T extends Object & Interface<? super T>> void foo(T a, T b) {
    // asSuper doesn't find Interface, so the type variable F is not substituted
    // causing isSuptype to be called between Object & Interface and F.
    a.compareTo(b);
  }

  interface Interface<F> {
    void compareTo(F t);
  }
}
