import checkers.nullness.quals.*;
import checkers.quals.*;

// This is a test case for (now fixed) issue #105:
// http://code.google.com/p/checker-framework/issues/detail?id=105

public class Uninit12 {

  static Object f;

  public Uninit12() {
    f.toString();
  }
  
  static Object g = new Object();

  static Object h;

  //:: error: (commitment.fields.uninitialized)
  static {
    h = new Object();
  }

}

class Uninit12_OK {
    
    static Object g = new Object();

    static Object h;

    static {
      h = new Object();
    }

}
