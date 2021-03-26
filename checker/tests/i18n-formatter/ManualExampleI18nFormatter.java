// Example from the manual

import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.DATE;
import static org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory.NUMBER;

import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

public class ManualExampleI18nFormatter {

  void m(boolean flag) {

    @I18nFormat({NUMBER, DATE}) String f;

    f = "{0, number, #.#} {1, date}"; // OK
    f = "{0, number} {1}"; // OK, GENERAL is weaker (less restrictive) than DATE
    f = "{0} {1, date}"; // OK, GENERAL is weaker (less restrictive) than NUMBER
    // :: warning: (i18nformat.missing.arguments)
    f = "{0, number}"; // warning: last argument is ignored
    // :: warning: (i18nformat.missing.arguments)
    f = "{0}"; // warning: last argument is ignored
    // :: warning: (i18nformat.missing.arguments)
    f = flag ? "{0, number} {1}" : "{0, number}";

    if (flag) {
      f = "{0, number} {1}";
    } else {
      // :: warning: (i18nformat.missing.arguments)
      f = "{0, number}";
    }
    @I18nFormat({NUMBER, DATE}) String f2 = f;

    // :: error: (assignment.type.incompatible)
    f = "{0, number} {1, number}"; // error: NUMBER is stronger (more restrictive) than DATE
    // :: error: (i18nformat.excess.arguments) :: error: (assignment.type.incompatible)
    f = "{0} {1} {2}"; // error: too many arguments
  }
}
