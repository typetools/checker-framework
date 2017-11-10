import java.text.MessageFormat;
import java.util.Date;

public class Syntax {

    // Test 2.1.1: Missing '}' at end of message format (Unmatched braces in the
    // pattern)
    public static void unmatchedBraces() {
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, number", new Date(12));
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0}{", 1);

        // good
        // :: warning: (i18nformat.excess.arguments)
        MessageFormat.format("'{0{}", 1);
        // :: warning: (i18nformat.excess.arguments)
        MessageFormat.format("'{0{}'", 1);
    }

    // Test 2.1.2.1: The argument number needs to be an integer
    public static void integerRequired() {
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{{0}}", 1);
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0.2}", 1);

        // good
        // :: warning: (i18nformat.excess.arguments)
        MessageFormat.format("'{{0}}'", 1);
    }

    // Test 2.1.2.2: The argument number can't be negative
    public static void nonNegativeRequired() {
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{-1, number}", 1);
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{-123}", 1);

        // good
        MessageFormat.format("{0}", 1);
    }

    // Test 2.1.3: Format Style required for choice format
    public static void formatStyleRequired() {
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, choice}", 1);

        // good
        MessageFormat.format("{0, choice, 0#zero}", 1);
    }

    // Test 2.1.4: Wrong format Style
    public static void wrongFormatStyle() {
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, time, number}", 1);
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, number, y.m.d}", 1);

        // good
        MessageFormat.format("{0, time, short}", 1);
        MessageFormat.format("{0, number, currency}", 1);
    }

    // Test 2.1.5: Unknown format type
    public static void unknownFormatType() {
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, general}", 1);
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, fool}", 1);

        // good
        MessageFormat.format("{0}", 1);
        MessageFormat.format("{0, time}", 1);
        MessageFormat.format("{0, date}", 1);
        MessageFormat.format("{0, number}", 1);
        MessageFormat.format("{0, daTe}", 1);
        MessageFormat.format("{0, NUMBER}", 1);
    }

    // Test 2.1.6: Invalid Subformat Pattern
    public static void invalidSubformatPattern() {
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, number, #.#.#}", 1);
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, date, y.m.d.x}", new Date());
        // :: error: (i18nformat.string.invalid)
        MessageFormat.format("{0, choice, 0##zero}", 0);

        // good
        MessageFormat.format("{0, number, #.#}", 1);
        MessageFormat.format("{0, date, y.m.d}", new Date());
        MessageFormat.format("{0, choice, 0>zero}", 0);
    }
}
