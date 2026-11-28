// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A binding variable nested in a record pattern is not defaulted, so a binding variable whose type
// is a type variable has the type of that type variable, which might be nullable.  The `accessor`
// method shows the expected behavior.
public class NestedBindingVariableTypeVariable {

  record Box<T>(T t) {}

  <T> void accessor(Box<T> b) {
    // :: error: [dereference.of.nullable]
    b.t().toString();
  }

  <T> void instanceOf(Box<T> b) {
    if (b instanceof Box<T>(T t)) {
      // :: error: [dereference.of.nullable]
      t.toString();
    }
  }

  <T> void caseLabel(Box<T> b) {
    switch (b) {
      case Box<T>(T t) -> {
        // :: error: [dereference.of.nullable]
        t.toString();
      }
    }
  }
}
