// @below-java17-jdk-skip-test
import org.checkerframework.checker.initialization.qual.UnderInitialization;

// A binding variable is not refined to bottom, even if the variable's default qualifier
// (@Initialized) and the tested expression's qualifier (@UnderInitialization) have no common
// subtype other than bottom.
public class InstanceOfBindingVariableInitialization {

  static class Sub extends InstanceOfBindingVariableInitialization {}

  Object f;

  InstanceOfBindingVariableInitialization() {
    if (this instanceof Sub s) {
      // Neither @Initialized nor @UnderInitialization(Object.class) is a subtype of the declared
      // type of `q`; only bottom is.
      // :: error: [assignment]
      @UnderInitialization(Sub.class) Object q = s;
    }
    f = "";
  }
}
