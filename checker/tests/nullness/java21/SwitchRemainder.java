// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that an exhaustive switch throws MatchException for a value that matches no case, such as a
// remainder (JLS 14.11.1.1).

import org.checkerframework.checker.nullness.qual.Nullable;

public class SwitchRemainder {

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  // R(null) matches neither R(Circle c) nor R(Square q).
  record R(@Nullable Shape s) {}

  enum E {
    A,
    B
  }

  void recordRemainder(R r) {
    @Nullable Object x = "";
    try {
      x.toString();
      x = null;
      switch (r) {
        case R(Circle c) -> {}
        case R(Square q) -> {}
      }
      x = "";
    } catch (MatchException e) {
      // :: error: [dereference.of.nullable]
      x.hashCode();
    }
  }

  void sealedRemainder(Shape s) {
    @Nullable Object x = "";
    try {
      x.toString();
      x = null;
      switch (s) {
        case Circle c -> {}
        case Square q -> {}
      }
      x = "";
    } catch (MatchException e) {
      // :: error: [dereference.of.nullable]
      x.hashCode();
    }
  }

  int enumRemainder(E e) {
    @Nullable Object x = "";
    try {
      x.toString();
      x = null;
      int result =
          switch (e) {
            case A -> 1;
            case B -> 2;
          };
      x = "";
      return result;
    } catch (MatchException ex) {
      // :: error: [dereference.of.nullable]
      return x.hashCode();
    }
  }

  // The last case matches every non-null Shape, so the switch has no remainder.
  void noRemainder(Shape s) {
    @Nullable Object x = "";
    try {
      x.toString();
      x = null;
      switch (s) {
        case Circle c -> {}
        case Shape t -> {}
      }
      x = "";
    } catch (MatchException e) {
      x.hashCode();
    }
  }

  // After the switch, the facts established by every case hold.
  void factsAfterSwitch(Shape s) {
    @Nullable Object x = null;
    switch (s) {
      case Circle c -> x = "";
      case Square q -> x = "";
    }
    x.hashCode();
  }
}
