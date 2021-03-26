public class AssertNullable {
  public static void main(String[] args) {
    if (args.length >= 1) {
      Boolean b = null;
      // This will result in an NPE, not an AssertionError:
      // Exception in thread "main" java.lang.NullPointerException
      // Therefore, the Nullness Checker warns about this.
      // :: error: (condition.nullable)
      assert b;
    } else {
      String s = null;
      // This is OK, the message will look like:
      // Exception in thread "main" java.lang.AssertionError: null
      assert 4 < 3 : s;
    }
  }

  void foo() {
    String s = 3 > 2 ? null : "ba";
    // :: error: (dereference.of.nullable)
    assert s.hashCode() > 4;
  }
}
