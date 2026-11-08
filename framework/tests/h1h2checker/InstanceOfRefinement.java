// @below-java17-jdk-skip-test
import org.checkerframework.framework.testchecker.h1h2checker.quals.*;

// The reference type of an instanceof refines the tested expression in each hierarchy in which a
// qualifier is written, even if the reference type has no written qualifier in another hierarchy.
public class InstanceOfRefinement {

  void bindingPattern(@H1Top @H2S1 Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof @H1S1 String s) {
      @H1S1 @H2S1 Object x = o;
    }
  }

  void bindingPatternOtherHierarchy(@H1S1 @H2Top Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof @H2S1 String s) {
      @H1S1 @H2S1 Object x = o;
    }
  }

  void typeTree(@H1Top @H2S1 Object o) {
    // :: warning: [instanceof.unsafe]
    if (o instanceof @H1S1 String) {
      @H1S1 @H2S1 Object x = o;
    }
  }

  void noRefinementInOtherHierarchy(@H1Top @H2Top Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof @H1S1 String s) {
      // :: error: [assignment]
      @H2S1 Object x = o;
    }
  }
}
