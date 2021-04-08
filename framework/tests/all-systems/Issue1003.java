// Test case for Issue 1003
// https://github.com/typetools/checker-framework/issues/1003

// The fact that M extends Issue1003 is unimportant,
// I'm just doing this to wrap it up into a single class example:
public class Issue1003<M extends Issue1003> {
  public int field = 0;

  public M getFoo() {
    throw new RuntimeException();
  }

  public static void methodMemberAccess() {
    // Use of raw generics:
    Issue1003 m = new Issue1003();
    // This version causes error but not exception:
    // Issue1003<Issue1003> m = new Issue1003<>();
    // Exception caused by this line:
    int x = m.getFoo().field;
  }
}
