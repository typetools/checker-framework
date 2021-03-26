// Test case for Issue 144 (now fixed):
// https://github.com/typetools/checker-framework/issues/144
public class Uninit14 {
  private final Object o;

  {
    try {
      o = new Object();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
