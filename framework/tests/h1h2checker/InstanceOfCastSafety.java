// @below-java17-jdk-skip-test
import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

// The type-tree form and the binding-pattern form of an instanceof are equally safe.
public class InstanceOfCastSafety {

  void unrelatedQualifier(@H1S1 @H2Top Object o) {
    // :: warning: [instanceof.unsafe]
    if (o instanceof @H1S2 String) {}
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof @H1S2 String s) {}
  }

  void upcast(@H1S1 @H2Top Object o) {
    if (o instanceof @H1Top String) {}
    if (o instanceof @H1Top String s) {}
  }
}
