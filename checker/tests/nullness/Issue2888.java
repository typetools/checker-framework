import com.sun.istack.internal.Nullable;

public class Issue2888 {
  @Nullable Object[] noa;

  void foo() {
    noa = null;
    // :: error: (accessing.nullable) :: error: (assignment)
    noa[0] = null;
  }

  @Nullable Object[] foo2(@Nullable Object[] p) {
    noa = p;
    noa = foo2(noa);
    noa = foo2(p);
    return p;
  }

  // The below is copied from Issue 2923.
  public void bar1(@Nullable String... args) {
    bar2(args);
  }

  private void bar2(@Nullable String... args) {
    if (args != null && args.length > 0) {
      @Nullable final String arg0 = args[0];
      // :: warning: (nulltest.redundant)
      if (arg0 != null) {
        System.out.println("arg0: " + arg0);
      }
    }
  }
}
