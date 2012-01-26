import checkers.nullness.quals.*;
import checkers.quals.*;

// This is a test case for issue #105:
// http://code.google.com/p/checker-framework/issues/detail?id=105

// TODO: This should generate an error regarding (non-)initialization of
// field f1, which is non-null, but is never initialized.
// Fields f2 and f3 are OK.

public class Uninit12 {

  static Object f;

  public Uninit12() {
    f.toString();
  }
  
  static Object g = new Object();

  static Object h;

  static {
    h = new Object();
  }

}

