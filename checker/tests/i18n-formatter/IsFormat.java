import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.util.I18nFormatUtil;

import java.text.MessageFormat;

public class IsFormat {
    public static void test1(String cc) {
        if (!I18nFormatUtil.isFormat(cc)) {
            // :: error: (i18nformat.string.invalid)
            MessageFormat.format(cc, "A");
        } else {
            // :: error: (i18nformat.string.invalid)
            MessageFormat.format(cc, "A");
            if (I18nFormatUtil.hasFormat(cc, I18nConversionCategory.GENERAL)) {
                MessageFormat.format(cc, "A");
            } else {
                // :: error: (i18nformat.string.invalid)
                MessageFormat.format(cc, "A");
            }
        }
    }

    public static void test2(String cc) {
        if (!I18nFormatUtil.isFormat(cc)) {
            // :: error: (i18nformat.string.invalid)
            MessageFormat.format(cc, "A");
        } else {
            // :: error: (i18nformat.string.invalid)
            MessageFormat.format(cc, "A");
            if (I18nFormatUtil.hasFormat(cc, I18nConversionCategory.NUMBER)) {
                MessageFormat.format(cc, 1);
            } else {
                // :: error: (i18nformat.string.invalid)
                MessageFormat.format(cc, "A");
            }
        }
    }
}
