// @below-java17-jdk-skip-test
import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintingBindingVariable {

  void bar(@Untainted Object o) {
    if (o instanceof @Untainted String s) {
      @Untainted String f = s;
    }
    if (o instanceof String s) {
      @Untainted String f2 = s;
    }
  }

  void bar2(Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof @Untainted String s) {
      @Untainted String f = s;
      @Untainted Object g = o;
    } else {
      // :: error: [assignment]
      @Untainted Object g = o;
    }
    if (o instanceof String s) {
      // :: error: [assignment]
      @Untainted String f2 = s;
    }
  }

  void typeAnnotationOnBindingType(Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (o instanceof java.lang.@Untainted String s) {
      @Untainted String f = s;
      @Untainted Object g = o;
    }
  }

  void negatedAnnotated(Object o) {
    // :: warning: [instanceof.pattern.unsafe]
    if (!(o instanceof @Untainted String s)) {
      // :: error: [assignment]
      @Untainted Object g = o;
      return;
    }
    @Untainted Object g = o;
  }

  void negated(@Untainted Object o) {
    if (!(o instanceof String s)) {
      return;
    }
    @Untainted String f = s;
  }

  void negated2(Object o) {
    if (!(o instanceof String s)) {
      return;
    }
    // :: error: [assignment]
    @Untainted String f = s;
  }

  // An upcast is safe, whether written as a type tree or as a binding pattern.
  void upcast(@Untainted String a) {
    if (a instanceof @Tainted String) {
      @Untainted String f = a;
    }
    if (a instanceof @Tainted String s) {
      @Untainted String f = a;
    }
  }
}
