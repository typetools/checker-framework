import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nFormat;

public class ConversionCategoryTest {

  public static void main(String[] args) {
    @I18nFormat({I18nConversionCategory.GENERAL}) String s1 = "{0}";

    @I18nFormat({I18nConversionCategory.DATE}) String s2 = "{0, date}";
    @I18nFormat({I18nConversionCategory.NUMBER}) String s3 = "{0, number}";

    @I18nFormat({I18nConversionCategory.NUMBER, I18nConversionCategory.NUMBER}) String s4 = "{1} {0, date}";
    // :: warning: (i18nformat.missing.arguments)
    s4 = "{0}";

    @I18nFormat({I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER}) String s5 = "{0} and {1, number}";
    @I18nFormat({I18nConversionCategory.UNUSED, I18nConversionCategory.NUMBER}) String s6 = "{1, number}";
    @I18nFormat({I18nConversionCategory.UNUSED, I18nConversionCategory.DATE}) String s7 = "{1, date}";

    @I18nFormat({
      I18nConversionCategory.UNUSED,
      I18nConversionCategory.UNUSED,
      I18nConversionCategory.NUMBER
    })
    String s8 = "{2}";

    @I18nFormat({
      I18nConversionCategory.GENERAL,
      I18nConversionCategory.DATE,
      I18nConversionCategory.UNUSED,
      I18nConversionCategory.NUMBER
    })
    String s9 = "{3, number} {0} {1, time}";

    @I18nFormat({
      I18nConversionCategory.GENERAL,
      I18nConversionCategory.DATE,
      I18nConversionCategory.DATE,
      I18nConversionCategory.NUMBER,
      I18nConversionCategory.UNUSED,
      I18nConversionCategory.GENERAL
    })
    String s10 = "{0} {1, date} {2, time} {3, number} {5}";

    @I18nFormat({I18nConversionCategory.UNUSED, I18nConversionCategory.DATE}) String s11 = "{1} {1, date}";

    @I18nFormat({I18nConversionCategory.UNUSED, I18nConversionCategory.NUMBER}) String s12 = "{1, number} {1, date}";

    @I18nFormat({I18nConversionCategory.DATE}) String s13 = "{0, date} {0, date}";

    // :: error: (i18nformat.excess.arguments) :: error: (assignment)
    @I18nFormat({I18nConversionCategory.GENERAL}) String b1 = "{1}";

    // :: error: (assignment)
    @I18nFormat({I18nConversionCategory.DATE}) String b2 = "{0, number}";

    // :: error: (assignment)
    @I18nFormat({I18nConversionCategory.GENERAL}) String b3 = "{0, number}";

    // :: error: (assignment)
    @I18nFormat({I18nConversionCategory.GENERAL}) String b4 = "{0, date}";

    // :: error: (i18nformat.excess.arguments) :: error: (assignment)
    @I18nFormat({I18nConversionCategory.DATE}) String b5 = "{0, date} {1, date}";

    // :: warning: (i18nformat.missing.arguments)
    @I18nFormat({I18nConversionCategory.DATE, I18nConversionCategory.DATE}) String b6 = "{0, date}";
  }
}
