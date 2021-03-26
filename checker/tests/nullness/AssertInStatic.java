public class AssertInStatic {

  static {
    long x = 0;
    try {
      x = 0;
    } catch (Throwable e) {
      assert true;
    }
  }
}
