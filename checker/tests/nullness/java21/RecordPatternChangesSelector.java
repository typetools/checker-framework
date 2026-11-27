import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.SideEffectFree;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Matching a record pattern invokes the record's component accessors, which can change the value
// of a selector field.  So a switch does not refine a selector field to non-null if a record
// accessor that a case label invokes might have side effects.
public class RecordPatternChangesSelector {

  static @Nullable Object selector;

  record Clearing(int n) {
    @Override
    public int n() {
      selector = null;
      return n;
    }
  }

  record Implicit(int n) {}

  record Annotated(int n) {
    @Override
    @SideEffectFree
    public int n() {
      return n;
    }
  }

  record Outer(Object inner) {}

  void clearing() {
    switch (selector) {
      case Clearing(int n) -> {
        // :: error: [dereference.of.nullable]
        selector.toString();
      }
      default -> {
        // This error is a false positive:  no accessor is invoked unless the record pattern
        // matches, so the selector has not been cleared here.  The analysis is conservative.
        // :: error: [dereference.of.nullable]
        selector.toString();
      }
    }
  }

  void clearingNested() {
    switch (selector) {
      case Outer(Clearing(int n)) -> {
        // :: error: [dereference.of.nullable]
        selector.toString();
      }
      default -> {
        // :: error: [dereference.of.nullable]
        selector.toString();
      }
    }
  }

  void clearingLocal(@Nullable Object local) {
    switch (local) {
      case Clearing(int n) -> local.toString();
      default -> local.toString();
    }
  }

  void implicit() {
    switch (selector) {
      case Implicit(int n) -> selector.toString();
      default -> selector.toString();
    }
  }

  void annotated() {
    switch (selector) {
      case Annotated(int n) -> selector.toString();
      default -> selector.toString();
    }
  }
}
