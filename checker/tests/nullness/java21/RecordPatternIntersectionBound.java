import java.io.Serializable;
import java.util.RandomAccess;
import org.checkerframework.checker.nullness.qual.Nullable;

// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// The Java type of a `var` binding variable can be an intersection type whose bounds come from both
// a wildcard bound and the declared bound of a type parameter, in either order.
public class RecordPatternIntersectionBound {

  record Box<T extends @Nullable CharSequence & Serializable>(T value) {}

  void explicitType(Box<? extends Comparable<String>> b) {
    if (b instanceof Box<? extends Comparable<String>>(var x)) {
      x.toString();
      int i = x.compareTo("");
    }
  }

  void inferredType(Box<? extends Comparable<String>> b) {
    if (b instanceof Box(var x)) {
      x.toString();
    }
  }

  void nullable(Box<? extends @Nullable Comparable<String>> b) {
    if (b instanceof Box(var x)) {
      // :: error: [dereference.of.nullable]
      x.toString();
    }
  }

  void randomAccess(Box<? extends RandomAccess> b) {
    if (b instanceof Box<? extends RandomAccess>(var x)) {
      x.toString();
    }
  }

  void sameBoundsOtherOrder(Box<? extends Serializable> b) {
    if (b instanceof Box<? extends Serializable>(var x)) {
      x.toString();
    }
  }

  void caseLabel(Box<? extends @Nullable Comparable<String>> b) {
    switch (b) {
      case Box(var x) -> {
        // :: error: [dereference.of.nullable]
        x.toString();
      }
      default -> {}
    }
  }
}
