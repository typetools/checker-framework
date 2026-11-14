import org.checkerframework.checker.interning.qual.Interned;

// @below-java21-jdk-skip-test

// A switch compares its selector to a case constant by value (or, for a String, by equals()), not
// by reference identity.  Therefore, a case constant does not refine the selector to @Interned.
public class SwitchCaseInterning {

  boolean boxed(Integer i, @Interned Integer other) {
    switch (i) {
      case 1000 -> {
        // :: error: (not.interned)
        return i == other;
      }
      default -> {
        return false;
      }
    }
  }

  boolean boxedCaseNull(Integer i, @Interned Integer other) {
    switch (i) {
      case null -> {
        return false;
      }
      case 1000 -> {
        // :: error: (not.interned)
        return i == other;
      }
      default -> {
        return false;
      }
    }
  }

  boolean string(String s, @Interned String other) {
    switch (s) {
      case "a" -> {
        // :: error: (not.interned)
        return s == other;
      }
      default -> {
        return false;
      }
    }
  }
}
