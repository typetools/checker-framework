import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// A switch without a `case null` label throws NullPointerException if its selector is null, so the
// selector is non-null after the switch.
public class SwitchSelectorNonNull {

  enum Color {
    RED,
    GREEN
  }

  @NonNull String string(@Nullable String s) {
    // :: error: [switching.nullable]
    switch (s) {
      default:
        break;
    }
    return s;
  }

  @NonNull String stringCase(@Nullable String s) {
    // :: error: [switching.nullable]
    switch (s) {
      case "a":
        return s;
      default:
        return s;
    }
  }

  @NonNull Color enumSelector(@Nullable Color c) {
    // :: error: [switching.nullable]
    switch (c) {
      case RED:
        break;
      default:
        break;
    }
    return c;
  }

  @NonNull Integer boxed(@Nullable Integer i) {
    // :: error: [switching.nullable]
    switch (i) {
      default:
        break;
    }
    return i;
  }

  @NonNull Integer boxedAssignment(@Nullable Integer i, @Nullable Integer j) {
    // :: error: [switching.nullable]
    switch (i = j) {
      default:
        break;
    }
    i.toString();
    return j;
  }

  @NonNull Object boxedCast(@Nullable Object o) {
    // :: error: [switching.nullable]
    switch ((Integer) o) {
      default:
        break;
    }
    return o;
  }

  @NonNull String assignment(@Nullable String s, @Nullable String t) {
    // :: error: [switching.nullable]
    switch (s = t) {
      default:
        break;
    }
    return s;
  }

  @NonNull Object cast(@Nullable Object o) {
    // :: error: [switching.nullable]
    switch ((String) o) {
      default:
        break;
    }
    return o;
  }

  @NonNull Object assignmentOfCast(@Nullable String s, @Nullable Object o) {
    // :: error: [switching.nullable]
    switch (s = (String) o) {
      default:
        break;
    }
    return o;
  }

  @NonNull Object castOfAssignment(@Nullable Object s, @Nullable Object t) {
    // :: error: [switching.nullable]
    switch ((String) (s = t)) {
      default:
        break;
    }
    s.toString();
    return t;
  }

  @NonNull Object assignmentOfCastOfAssignment(
      @Nullable String s, @Nullable Object u, @Nullable Object t) {
    // :: error: [switching.nullable]
    switch (s = (String) (u = t)) {
      default:
        break;
    }
    s.toString();
    u.toString();
    return t;
  }

  void concatenation(@Nullable String s) {
    switch ("a" + s) {
      default:
        break;
    }
  }

  // The NullPointerException is an exceptional exit from the switch, so the selector may be null in
  // the handler.
  void caught(@Nullable String s) {
    try {
      // :: error: [switching.nullable]
      switch (s) {
        default:
          break;
      }
    } catch (NullPointerException e) {
      // :: error: [dereference.of.nullable]
      s.length();
    }
  }
}
