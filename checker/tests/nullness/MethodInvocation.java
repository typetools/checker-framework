import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;

public class MethodInvocation {

  String s;

  public MethodInvocation() {
    // :: error: (method.invocation.invalid)
    a();
    b();
    c();
    s = "abc";
  }

  public MethodInvocation(boolean p) {
    // :: error: (method.invocation.invalid)
    a(); // still not okay to be initialized
    s = "abc";
  }

  public void a() {}

  public void b(@UnderInitialization MethodInvocation this) {
    // :: error: (dereference.of.nullable)
    s.hashCode();
  }

  public void c(@UnknownInitialization MethodInvocation this) {
    // :: error: (dereference.of.nullable)
    s.hashCode();
  }
}
