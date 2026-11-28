import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// A nested binding variable whose type is a type variable has the type of the corresponding record
// component, so the binding variable may be null if the type variable's upper bound is @Nullable.
public class RecordPatternTypeVariable {

  record Box<T>(T value) {}

  <T extends @Nullable Object> void varPattern(Box<T> b) {
    if (b instanceof Box<T>(var value)) {
      // :: error: [dereference.of.nullable]
      value.toString();
    }
  }

  <T extends @Nullable Object> void explicitPattern(Box<T> b) {
    if (b instanceof Box<T>(T value)) {
      // :: error: [dereference.of.nullable]
      value.toString();
    }
  }

  <T extends @Nullable Object> void caseLabel(Box<T> b) {
    switch (b) {
      case Box<T>(var value) -> {
        // :: error: [dereference.of.nullable]
        value.toString();
      }
    }
  }

  <T extends Object> void nonNullBound(Box<T> b) {
    if (b instanceof Box<T>(var value)) {
      value.toString();
    }
  }
}
