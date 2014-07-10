// Test case for Issue 144 (now fixed):
// http://code.google.com/p/checker-framework/issues/detail?id=144
public class Uninit14 {
  private final Object o;
  {
    try {
      o = new Object( );
    } catch( Exception e ) {
      throw new RuntimeException( e );
    }
  }
}