import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 languages features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// An unannotated pattern variable gets the same default as a local variable, so it has the
// qualifiers of the matched value.  The local variable in each `local*` method shows the expected
// behavior.
public class PatternVariableDefaults {

  // Integer has a type-based default, @Signed, which does not apply to the top level of a local
  // variable's type.

  void localBoxed(@Unsigned Integer u) {
    Object o = u;
    Integer i = (Integer) o;
    // :: error: (operation.unsignedlhs)
    int q = i / 2;
  }

  void caseLabelBoxed(@Unsigned Integer u) {
    Object o = u;
    switch (o) {
      case Integer i -> {
        // :: error: (operation.unsignedlhs)
        int q = i / 2;
      }
      default -> {}
    }
  }

  void instanceOfBoxed(@Unsigned Integer u) {
    Object o = u;
    if (o instanceof Integer i) {
      // :: error: (operation.unsignedlhs)
      int q = i / 2;
    }
  }

  // A pattern variable whose type is a type variable.

  <T extends Number> void localTypeVariable(@Unsigned T x) {
    T t = x;
    @Unsigned T u = t;
    // :: error: (assignment)
    @Signed T s = t;
  }

  <T extends Number> void caseLabelTypeVariable(@Unsigned T x) {
    switch (x) {
      case T t -> {
        @Unsigned T u = t;
        // :: error: (assignment)
        @Signed T s = t;
      }
    }
  }

  <T extends Number> void instanceOfTypeVariable(@Unsigned T x) {
    if (x instanceof T t) {
      @Unsigned T u = t;
      // :: error: (assignment)
      @Signed T s = t;
    }
  }
}
