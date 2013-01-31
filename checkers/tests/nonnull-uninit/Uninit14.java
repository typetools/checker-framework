// Test case for Issue 144:
// http://code.google.com/p/checker-framework/issues/detail?id=144
// Field o should be counted as initialized, but isn't.
//@skip-test
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