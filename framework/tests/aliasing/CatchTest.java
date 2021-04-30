import org.checkerframework.common.aliasing.qual.Unique;

public class CatchTest {

  void foo() {
    @Unique Exception exVar = new Exception();
    try {
      // :: error: (unique.leaked)
      throw exVar;

      // :: error: (exception.parameter)
    } catch (@Unique Exception e) {
      // exVar and e points to the same object, therefore catch clauses
      // are not allowed to have a @Unique parameter.
    }
  }
}
