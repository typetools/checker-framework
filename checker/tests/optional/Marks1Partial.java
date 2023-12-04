import java.util.Optional;

/**
 * Partial test case for rule #1: "Never, ever, use null for an Optional variable or return value."
 *
 * <p>Warnings for assignment of null values to Optional types are handled by the Nullness Checker.
 * This is a partial test suite for testing whether the Optional Checker detects comparisons of
 * Optional values to null literals.
 */
public class Marks1Partial {

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
}
