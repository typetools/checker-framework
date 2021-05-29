import org.checkerframework.checker.nullness.qual.*;

public class Throwing {

  String a;

  // :: error: (initialization.fields.uninitialized)
  public Throwing(boolean throwError) {
    if (throwError) {
      throw new RuntimeException("not a real error");
    }
  }

  // :: error: (initialization.fields.uninitialized)
  public Throwing(int input) {
    try {
      throw new RuntimeException("not a real error");
    } catch (RuntimeException e) {
      // do nothing
    }
  }
}
