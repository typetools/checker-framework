// Test case for issue 689
// https://github.com/typetools/checker-framework/issues/689
public class Issue689 {

  public int initializerFodder() {
    return 3;
  }

  public Runnable trigger() {
    return new Runnable() {
      // Issue 689 triggers when examining a method invocation inside a field initializer of
      // an anonymous inner class
      public final int val = initializerFodder();

      public void run() {
        // do nothing
      }
    };
  }
}
