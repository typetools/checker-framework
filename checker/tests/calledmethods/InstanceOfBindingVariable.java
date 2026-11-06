// @below-java17-jdk-skip-test
import org.checkerframework.checker.calledmethods.qual.CalledMethods;

// The binding variable of an instanceof has both the tested expression's type and its own declared
// type.
class InstanceOfBindingVariable {

  void a() {}

  void b() {}

  void declaredTypeIsMoreSpecific(Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof @CalledMethods("a") InstanceOfBindingVariable x) {
      x.b();
      @CalledMethods({"a", "b"}) InstanceOfBindingVariable y = x;
    }
  }

  void expressionTypeIsMoreSpecific(@CalledMethods("a") Object o) {
    if (o instanceof InstanceOfBindingVariable x) {
      x.b();
      @CalledMethods({"a", "b"}) InstanceOfBindingVariable y = x;
    }
  }
}
