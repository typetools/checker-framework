import org.checkerframework.checker.initialization.qual.UnderInitialization;
import org.checkerframework.checker.nullness.qual.*;

public class FlowConstructor {

  String a;
  String b;

  public FlowConstructor(float f) {
    a = "m";
    b = "n";
    semiRawMethod();
  }

  public FlowConstructor(int p) {
    a = "m";
    b = "n";
    // :: error: (method.invocation.invalid)
    nonRawMethod();
  }

  public FlowConstructor(double p) {
    a = "m";
    // :: error: (method.invocation.invalid)
    nonRawMethod(); // error
    b = "n";
  }

  void nonRawMethod() {
    a.toString();
    b.toString();
  }

  void semiRawMethod(@UnderInitialization(FlowConstructor.class) FlowConstructor this) {
    a.toString();
    b.toString();
  }
}
