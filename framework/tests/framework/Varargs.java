import org.checkerframework.framework.testchecker.util.*;

public class Varargs {
  public void testVarargsInvocation() {
    @Odd String s = null;
    aVarargsMethod(s);
    // :: error: (argument.type.incompatible)
    aVarargsMethod(s, "");
    aVarargsMethod(s, s);

    moreVarargs(new @Odd String[1]);
    // The assignment context infers @Odd for the component type.  With invariant array subtyping,
    // this will fail, as the main type is a subtype.
    moreVarargs(new String @Odd [1]);
    // :: warning: (cast.unsafe.constructor.invocation)
    moreVarargs(new @Odd String(), new @Odd String());
    // :: error: (argument.type.incompatible)
    // :: warning: (cast.unsafe.constructor.invocation)
    moreVarargs(new String(), new @Odd String());
    moreVarargs(
        // :: error: (argument.type.incompatible)
        new String(),
        // :: error: (argument.type.incompatible)
        new String());
  }

  /* ------------------------------------------------------------ */

  public void aVarargsMethod(@Odd String s, @Odd String... more) {}

  public void moreVarargs(@Odd String... args) {}

  Varargs(String... args) {}

  void test() {
    new Varargs("m", "n");
    new Varargs();
  }

  void testVarargsConstructor() {
    new ProcessBuilder("hello");
  }
}
