import java.text.MessageFormat;
import java.util.Date;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.util.I18nFormatUtil;

public class HasFormat {

  void test1(String format) {
    if (I18nFormatUtil.hasFormat(
        format, I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER)) {
      MessageFormat.format(format, "S", 1);
      // :: warning: (i18nformat.missing.arguments)
      MessageFormat.format(format, "S");
      // :: error: (argument.type.incompatible)
      MessageFormat.format(format, "S", "S");
      // :: warning: (i18nformat.excess.arguments)
      MessageFormat.format(format, "S", 1, 2);
    }
  }

  void test2(String format) {
    if (!I18nFormatUtil.hasFormat(
        format, I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER)) {
      // :: error: (i18nformat.string.invalid)
      MessageFormat.format(format, "S", 1);
    }
  }

  void test3(String format) {
    if (I18nFormatUtil.hasFormat(
        format,
        I18nConversionCategory.GENERAL,
        I18nConversionCategory.UNUSED,
        I18nConversionCategory.GENERAL)) {
      // :: warning: (i18nformat.argument.unused)
      MessageFormat.format(format, "S", 1, "S");
    }
  }

  void test4(String format) throws Exception {
    // :: error: (i18nformat.string.invalid)
    MessageFormat.format(format, "S");
    if (I18nFormatUtil.hasFormat(format, I18nConversionCategory.GENERAL)) {
      MessageFormat.format(format, "S");
      MessageFormat.format(format, new Date());
      MessageFormat.format(format, 1);
    } else {
      throw new Exception();
    }
  }

  void tes5(String format) {
    if (I18nFormatUtil.hasFormat(format, I18nConversionCategory.NUMBER)) {
      // :: error: (argument.type.incompatible)
      MessageFormat.format(format, "S");
      MessageFormat.format(format, 1);
    } else {
      // :: error: (i18nformat.string.invalid)
      MessageFormat.format(format, 1);
    }
  }
}
