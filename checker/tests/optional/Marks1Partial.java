import java.util.Optional;

/**
 * Test case for rule #1: "Never, ever, use null for an Optional variable or return value."
 *
 * <p>Warnings for assignment of null values to Optional types are handled by the Nullness Checker.
 * This is a partial test suite for testing whether the Optional Checker detects comparisons of
 * Optional values to null literals.
 */
public class Marks1Partial {

  @SuppressWarnings("optional.field")
  Optional<String> optField = Optional.ofNullable("f1");

  @SuppressWarnings("optional.parameter")
  void simpleEqualsCheck(Optional<String> o1) {
    // :: warning: (optional.null.comparison)
    if (o1 != null) {
      System.out.println("Don't compare optionals (lhs) to null literals.");
    }
    // :: warning: (optional.null.comparison)
    if (null != o1) {
      System.out.println("Don't compare optionals (rhs) to null literals.");
    }
    // :: warning: (optional.null.comparison)
    if (o1 == null) {
      System.out.println("Don't compare optionals (lhs) to null literals.");
    }
    // :: warning: (optional.null.comparison)
    if (null == o1) {
      System.out.println("Don't compare optionals (rhs) to null literals.");
    }
  }

  @SuppressWarnings("optional.parameter")
  void moreComplexEqualsChecks(Optional<String> o1) {
    // :: warning: (optional.null.comparison)
    if (o1 != null || 1 + 2 == 4) {
      System.out.println("Don't compare optionals (lhs) to null literals.");
    }
  }

  @SuppressWarnings("optional.parameter")
  void checkAgainstOptionalField() {
    // :: warning: (optional.null.comparison)
    if (this.getOptField() != null || 1 + 2 == 4) {
      System.out.println("Don't compare optionals (lhs) to null literals.");
    }
  }

  public Optional<String> getOptField() {
    return optField;
  }

  public void assignOptField() {
    // :: warning: (optional.null.assignment)
    optField = null;
  }

  public void assignOptionalDeclaration() {
    // :: warning: (optional.null.assignment)
    Optional<String> os1 = null;
    Optional<String> os2;
    if (Math.random() > 0.5) {
      os2 = Optional.of("hello");
    } else {
      // :: warning: (optional.null.assignment)
      os2 = null;
    }
    // :: warning: (optional.null.assignment)
    Optional<String> os3 = Math.random() > 0.5 ? Optional.of("hello") : null;
  }

  public Optional<String> returnNullOptional() {
    // :: warning: (optional.null.assignment)
    return null;
  }
}
