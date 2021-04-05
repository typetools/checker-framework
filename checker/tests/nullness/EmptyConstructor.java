// @skip-test Change error key to one with a clearer message that explicitly mentions the superclass
// constructor

import org.checkerframework.dataflow.qual.*;

public class SuperClass {
  static int count = 0;

  public SuperClass() {
    count++;
  }
}

// The error message is very confusing:
//   EmptyConstructor.java:22: error: call to non-side-effect-free method not allowed in
// side-effect-free method
//     public EmptyConstructor() {}
//                               ^
// because there's no obvious call.  The message key should be changed to one whose message is "call
// to non-side-effect-free superclass constructor not allowed in side-effect-free constructor"

public class EmptyConstructor extends SuperClass {
  @SideEffectFree
  public EmptyConstructor() {}
}
