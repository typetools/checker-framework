// @below-java21-jdk-skip-test

// None of the WPI formats supports the new Java 21 language features, so skip inference until they
// do.
// @infer-jaifs-skip-test
// @infer-ajava-skip-test
// @infer-stubs-skip-test

// Test that the facts that hold when a case guard is false, and the effects of evaluating the
// guard, are not assumed when the case does not match, because the case also fails to match when
// its pattern does not match, in which case the guard is not evaluated.

import org.checkerframework.checker.nullness.qual.EnsuresNonNull;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CaseGuardElseStore {

  @Nullable Object f;

  @EnsuresNonNull("this.f")
  boolean init() {
    f = "";
    return true;
  }

  boolean clear() {
    f = null;
    return true;
  }

  @EnsuresNonNullIf(expression = "#1", result = true)
  static boolean isNN(@Nullable Object o) {
    return o != null;
  }

  @EnsuresNonNullIf(expression = "#1", result = false)
  static boolean isNull(@Nullable Object o) {
    return o == null;
  }

  void guardFalseOrPatternFails(Object sel, @Nullable Object o) {
    switch (sel) {
      case String s when isNull(o) -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        o.hashCode();
      }
    }
  }

  void guardFalseOrPatternFailsNextCase(Object sel, @Nullable Object o) {
    switch (sel) {
      case String s when isNull(o) -> {}
      case Integer i -> {
        // :: error: [dereference.of.nullable]
        o.hashCode();
      }
      default -> {}
    }
  }

  void guardTrue(Object sel, @Nullable Object o) {
    switch (sel) {
      case String s when isNN(o) -> {
        o.hashCode();
      }
      default -> {}
    }
  }

  void guardTrueConjunction(Object sel, @Nullable Object o, @Nullable Object p) {
    switch (sel) {
      case String s when isNN(o) && isNN(p) -> {
        o.hashCode();
        p.hashCode();
      }
      default -> {}
    }
  }

  void guardPostcondition(Object sel) {
    switch (sel) {
      case String s when init() -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
    }
  }

  void guardPostconditionNextCase(Object sel) {
    switch (sel) {
      case String s when init() -> {}
      case Integer i -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
      default -> {}
    }
  }

  void guardPostconditionConjunction(Object sel, @Nullable Object o) {
    switch (sel) {
      case String s when init() && isNull(o) -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
    }
  }

  void guardPostconditionPatternAlwaysMatches(String sel) {
    switch (sel) {
      case CharSequence s when init() -> {}
      default -> {
        f.hashCode();
      }
    }
  }

  void guardFalsePatternAlwaysMatches(String sel, @Nullable Object o) {
    switch (sel) {
      case String s when isNull(o) -> {}
      default -> {
        o.hashCode();
      }
    }
  }

  void guardFalsePatternAlwaysMatchesCaseNull(@Nullable String sel, @Nullable Object o) {
    switch (sel) {
      case String s when isNull(o) -> {}
      case null, default -> {
        // :: error: [dereference.of.nullable]
        o.hashCode();
      }
    }
  }

  int switchExpression(Object sel, @Nullable Object o) {
    return switch (sel) {
      case String s when isNN(o) -> o.hashCode();
      case Integer i when init() -> 0;
      default -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
        yield 1;
      }
    };
  }

  void statementGroup(Object sel) {
    switch (sel) {
      case String s
      when init():
        break;
      default:
        // :: error: [dereference.of.nullable]
        f.hashCode();
    }
  }

  sealed interface Shape permits Circle, Square {}

  record Circle() implements Shape {}

  record Square() implements Shape {}

  void noDefault(Shape sel) {
    switch (sel) {
      case Circle c when init() -> {}
      case Circle c -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
      case Square q -> {}
    }
  }

  record R(Shape s) {}

  // R(Circle c) and R(Square q) make the switch exhaustive, but R(null) matches only R(Shape s).
  void lastCaseOfExhaustiveGuardTrue(R r, @Nullable Object o) {
    switch (r) {
      case R(Circle c) -> {}
      case R(Square q) -> {}
      case R(Shape s) when isNN(o) -> {
        o.hashCode();
      }
    }
  }

  void lastCaseOfExhaustiveGuardEffects(R r) {
    f = "";
    switch (r) {
      case R(Circle c) -> {}
      case R(Square q) -> {}
      case R(Shape s) when clear() -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
    }
  }

  // The refinement of the selector comes from the label, not from the guard.
  void patternAlwaysMatchesRefinesSelector(@Nullable String sel, boolean b) {
    // :: error: [switching.nullable]
    switch (sel) {
      case String s when b -> {
        sel.hashCode();
      }
      default -> {}
    }
  }

  // If the guard of the last case is false, the switch throws MatchException.
  void lastCaseOfExhaustiveGuardFalse(R r, boolean b) {
    @Nullable Object x = null;
    switch (r) {
      case R(Circle c) -> x = "";
      case R(Square q) -> x = "";
      case R(Shape s) when b -> x = "";
    }
    x.hashCode();
  }

  void lastCaseOfExhaustiveGuardFalseCatch(R r, boolean b) {
    @Nullable Object x = "";
    try {
      x = null;
      switch (r) {
        case R(Circle c) -> x = "";
        case R(Square q) -> x = "";
        case R(Shape s) when b -> x = "";
      }
    } catch (MatchException e) {
      // :: error: [dereference.of.nullable]
      x.hashCode();
    }
  }

  void recordPatternAlwaysMatches(R r) {
    switch (r) {
      case R(Shape s) when init() -> {}
      default -> f.hashCode();
    }
  }

  void recordPatternVarAlwaysMatches(R r) {
    switch (r) {
      case R(var s) when init() -> {}
      default -> f.hashCode();
    }
  }

  void recordPatternNarrowerComponent(R r) {
    switch (r) {
      case R(Circle c) when init() -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
    }
  }

  record Box<T>(T t) {}

  void genericRecordPatternAlwaysMatches(Box<String> b) {
    switch (b) {
      case Box<String>(String s) when init() -> {}
      default -> f.hashCode();
    }
  }

  record P(int x) {}

  void primitiveComponentAlwaysMatches(P p) {
    switch (p) {
      case P(int x) when init() -> {}
      default -> f.hashCode();
    }
  }

  record RR(R r) {}

  // RR(null) does not match RR(R(Shape s)).
  void nestedRecordPatternDoesNotAlwaysMatch(RR rr) {
    switch (rr) {
      case RR(R(Shape s)) when init() -> {}
      default -> {
        // :: error: [dereference.of.nullable]
        f.hashCode();
      }
    }
  }
}
