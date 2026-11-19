import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;
import org.checkerframework.framework.qual.HasQualifierParameter;

// @below-java21-jdk-skip-test

// The qualifier of a binding variable whose type has a qualifier parameter must be the same as the
// qualifier of the value that the type pattern matches, as for a cast.  An unannotated binding
// variable whose type has a qualifier parameter gets the qualifier of the value that the type
// pattern matches, as a local variable gets the qualifier of its initializer.
public class TypePatternQualifierParameter {
  @HasQualifierParameter(Tainted.class)
  static class Buffer {}

  @HasQualifierParameter(Tainted.class)
  static class MyBuffer extends Buffer {}

  void instanceofPattern(@Tainted Buffer taintedBuf, @Untainted Buffer untaintedBuf) {
    if (taintedBuf instanceof @Tainted MyBuffer b) {}
    // :: warning: [instanceof.pattern.unsafe]
    if (untaintedBuf instanceof @Tainted MyBuffer b) {}
    if (untaintedBuf instanceof @Untainted MyBuffer b) {}
    // :: warning: [instanceof.pattern.unsafe]
    if (taintedBuf instanceof @Untainted MyBuffer b) {}
  }

  void instanceofPatternUnannotated(
      @Tainted Buffer taintedBuf, @Untainted Buffer untaintedBuf, @Tainted MyBuffer taintedMyBuf) {
    if (taintedBuf instanceof MyBuffer b) {
      @Tainted MyBuffer t = b;
      // :: error: [assignment]
      @Untainted MyBuffer u = b;
    }
    if (untaintedBuf instanceof MyBuffer b) {
      @Untainted MyBuffer u = b;
      // :: error: [assignment]
      b = taintedMyBuf;
    }
  }

  void caseLabel(@Tainted Buffer taintedBuf, @Untainted Buffer untaintedBuf) {
    switch (taintedBuf) {
      case @Tainted MyBuffer b -> {}
      default -> {}
    }
    switch (untaintedBuf) {
      // :: warning: [case.pattern.unsafe]
      case @Tainted MyBuffer b -> {}
      default -> {}
    }
    switch (untaintedBuf) {
      case @Untainted MyBuffer b -> {}
      default -> {}
    }
    switch (taintedBuf) {
      // :: warning: [case.pattern.unsafe]
      case @Untainted MyBuffer b -> {}
      default -> {}
    }
  }

  void caseLabelUnannotated(
      @Tainted Buffer taintedBuf, @Untainted Buffer untaintedBuf, @Tainted MyBuffer taintedMyBuf) {
    switch (taintedBuf) {
      case MyBuffer b -> {
        @Tainted MyBuffer t = b;
        // :: error: [assignment]
        @Untainted MyBuffer u = b;
      }
      default -> {}
    }
    switch (untaintedBuf) {
      case MyBuffer b -> {
        @Untainted MyBuffer u = b;
        // :: error: [assignment]
        b = taintedMyBuf;
      }
      default -> {}
    }
  }
}
