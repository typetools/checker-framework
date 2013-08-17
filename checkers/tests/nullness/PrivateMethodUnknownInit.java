import checkers.initialization.quals.*;
import checkers.nullness.quals.Raw;

public class PrivateMethodUnknownInit {

  int x;

  public PrivateMethodUnknownInit() {
    x = 1;
    m1();
    //:: error: (method.invocation.invalid)
    m2();
  }

  private void m1(@UnknownInitialization(PrivateMethodUnknownInit.class) @Raw(PrivateMethodUnknownInit.class) PrivateMethodUnknownInit this) { }

  public void m2() { }

}
