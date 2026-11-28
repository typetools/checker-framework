// A switch whose selector is a boxed primitive, or a type variable bounded by one, does not unbox
// the selector when the switch has a `case null` label.

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

@SuppressWarnings("all") // Just check for crashes.
public class BoxedSelectorCaseNull {

  int boxed(Integer i) {
    return switch (i) {
      case null -> 0;
      case 1 -> 1;
      default -> 2;
    };
  }

  <T extends Integer> int typeVariable(T t) {
    return switch (t) {
      case null -> 0;
      default -> 1;
    };
  }

  <T extends Integer> int typeVariableStatement(T t) {
    switch (t) {
      case null:
        return 0;
      default:
        return 1;
    }
  }
}
