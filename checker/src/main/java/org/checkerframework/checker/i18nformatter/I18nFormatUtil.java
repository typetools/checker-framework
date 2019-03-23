package org.checkerframework.checker.i18nformatter;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.checkerframework.checker.i18nformatter.qual.I18nChecksFormat;
import org.checkerframework.checker.i18nformatter.qual.I18nConversionCategory;
import org.checkerframework.checker.i18nformatter.qual.I18nValidFormat;

/**
 * This class provides a collection of utilities to ease working with i18n format strings.
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
public class I18nFormatUtil {

    /**
     * Throws an exception if the format is not syntactically valid.
     *
     * @param format the format string to parse
     */
    public static void tryFormatSatisfiability(String format) throws IllegalFormatException {
        MessageFormat.format(format, (Object[]) null);
    }

    /**
     * Returns a {@link I18nConversionCategory} for every conversion found in the format string.
     *
     * @param format the format string to parse
     * @throws IllegalFormatException if the format is not syntactically valid
     */
    public static I18nConversionCategory[] formatParameterCategories(String format)
            throws IllegalFormatException {
        tryFormatSatisfiability(format);
        I18nConversion[] cs = MessageFormatParser.parse(format);

        int maxIndex = -1;
        Map<Integer, I18nConversionCategory> conv = new HashMap<>();

        for (I18nConversion c : cs) {
            int index = c.index;
            conv.put(
                    index,
                    I18nConversionCategory.intersect(
                            c.category,
                            conv.containsKey(index)
                                    ? conv.get(index)
                                    : I18nConversionCategory.UNUSED));
            maxIndex = Math.max(maxIndex, index);
        }

        I18nConversionCategory[] res = new I18nConversionCategory[maxIndex + 1];
        for (int i = 0; i <= maxIndex; i++) {
            res[i] = conv.containsKey(i) ? conv.get(i) : I18nConversionCategory.UNUSED;
        }
        return res;
    }

    /**
     * Returns true if the format string is satisfiable, and if the format's parameters match the
     * passed {@link I18nConversionCategory}s. Otherwise an error is thrown.
     *
     * @param format a format string
     * @param cc a list of expected categories for the string's format specifiers
     * @return true if the format string's specifiers are the given categories, in order
     */
    // TODO introduce more such functions, see RegexUtil for examples
    @I18nChecksFormat
    public static boolean hasFormat(String format, I18nConversionCategory... cc) {
        I18nConversionCategory[] fcc = formatParameterCategories(format);
        if (fcc.length != cc.length) {
            return false;
        }

        for (int i = 0; i < cc.length; i++) {
            if (!I18nConversionCategory.isSubsetOf(cc[i], fcc[i])) {
                return false;
            }
        }
        return true;
    }

    @I18nValidFormat
    public static boolean isFormat(String format) {
        try {
            formatParameterCategories(format);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private static class I18nConversion {
        public int index;
        public I18nConversionCategory category;

        public I18nConversion(int index, I18nConversionCategory category) {
            this.index = index;
            this.category = category;
        }

        @Override
        public String toString() {
            return category.toString() + "(index: " + index + ")";
        }
    }

    private static class MessageFormatParser {

        public static int maxOffset;

        /** The locale to use for formatting numbers and dates. */
        private static Locale locale;

        /** An array of formatters, which are used to format the arguments. */
        private static List<I18nConversionCategory> categories;

        /**
         * The argument numbers corresponding to each formatter. (The formatters are stored in the
         * order they occur in the pattern, not in the order in which the arguments are specified.)
         */
        private static List<Integer> argumentIndices;

        /** The number of subformats. */
        private static int numFormat;

        // Indices for segments
        private static final int SEG_RAW = 0;
        private static final int SEG_INDEX = 1;
        private static final int SEG_TYPE = 2;
        private static final int SEG_MODIFIER = 3; // modifier or subformat

        // Indices for type keywords
        private static final int TYPE_NULL = 0;
        private static final int TYPE_NUMBER = 1;
        private static final int TYPE_DATE = 2;
        private static final int TYPE_TIME = 3;
        private static final int TYPE_CHOICE = 4;

        private static final String[] TYPE_KEYWORDS = {"", "number", "date", "time", "choice"};

        // Indices for number modifiers
        private static final int MODIFIER_DEFAULT = 0; // common in number and date-time
        private static final int MODIFIER_CURRENCY = 1;
        private static final int MODIFIER_PERCENT = 2;
        private static final int MODIFIER_INTEGER = 3;

        private static final String[] NUMBER_MODIFIER_KEYWORDS = {
            "", "currency", "percent", "integer"
        };

        private static final String[] DATE_TIME_MODIFIER_KEYWORDS = {
            "", "short", "medium", "long", "full"
        };

        public static I18nConversion[] parse(String pattern) {
            MessageFormatParser.categories = new ArrayList<>();
            MessageFormatParser.argumentIndices = new ArrayList<>();
            MessageFormatParser.locale = Locale.getDefault(Locale.Category.FORMAT);
            applyPattern(pattern);

            I18nConversion[] ret = new I18nConversion[MessageFormatParser.numFormat];
            for (int i = 0; i < MessageFormatParser.numFormat; i++) {
                ret[i] = new I18nConversion(argumentIndices.get(i), categories.get(i));
            }
            return ret;
        }

        private static void applyPattern(String pattern) {
            StringBuilder[] segments = new StringBuilder[4];
            // Allocate only segments[SEG_RAW] here. The rest are
            // allocated on demand.
            segments[SEG_RAW] = new StringBuilder();

            int part = SEG_RAW;
            MessageFormatParser.numFormat = 0;
            boolean inQuote = false;
            int braceStack = 0;
            maxOffset = -1;
            for (int i = 0; i < pattern.length(); ++i) {
                char ch = pattern.charAt(i);
                if (part == SEG_RAW) {
                    if (ch == '\'') {
                        if (i + 1 < pattern.length() && pattern.charAt(i + 1) == '\'') {
                            segments[part].append(ch); // handle doubles
                            ++i;
                        } else {
                            inQuote = !inQuote;
                        }
                    } else if (ch == '{' && !inQuote) {
                        part = SEG_INDEX;
                        if (segments[SEG_INDEX] == null) {
                            segments[SEG_INDEX] = new StringBuilder();
                        }
                    } else {
                        segments[part].append(ch);
                    }
                } else {
                    if (inQuote) { // just copy quotes in parts
                        segments[part].append(ch);
                        if (ch == '\'') {
                            inQuote = false;
                        }
                    } else {
                        switch (ch) {
                            case ',':
                                if (part < SEG_MODIFIER) {
                                    if (segments[++part] == null) {
                                        segments[part] = new StringBuilder();
                                    }
                                } else {
                                    segments[part].append(ch);
                                }
                                break;
                            case '{':
                                ++braceStack;
                                segments[part].append(ch);
                                break;
                            case '}':
                                if (braceStack == 0) {
                                    part = SEG_RAW;
                                    makeFormat(numFormat, segments);
                                    numFormat++;
                                    // throw away other segments
                                    segments[SEG_INDEX] = null;
                                    segments[SEG_TYPE] = null;
                                    segments[SEG_MODIFIER] = null;
                                } else {
                                    --braceStack;
                                    segments[part].append(ch);
                                }
                                break;
                            case ' ':
                                // Skip any leading space chars for SEG_TYPE.
                                if (part != SEG_TYPE || segments[SEG_TYPE].length() > 0) {
                                    segments[part].append(ch);
                                }
                                break;
                            case '\'':
                                inQuote = true;
                                segments[part].append(ch);
                                break;
                            default:
                                segments[part].append(ch);
                                break;
                        }
                    }
                }
            }
            if (braceStack == 0 && part != 0) {
                maxOffset = -1;
                throw new IllegalArgumentException("Unmatched braces in the pattern");
            }
        }

        /** Side-effects {@code categories} field, adding to it an I18nConversionCategory. */
        private static void makeFormat(int offsetNumber, StringBuilder[] textSegments) {
            String[] segments = new String[textSegments.length];
            for (int i = 0; i < textSegments.length; i++) {
                StringBuilder oneseg = textSegments[i];
                segments[i] = (oneseg != null) ? oneseg.toString() : "";
            }

            // get the argument number
            int argumentNumber;
            try {
                argumentNumber = Integer.parseInt(segments[SEG_INDEX]); // always
                // unlocalized!
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "can't parse argument number: " + segments[SEG_INDEX], e);
            }
            if (argumentNumber < 0) {
                throw new IllegalArgumentException("negative argument number: " + argumentNumber);
            }

            int oldMaxOffset = maxOffset;
            maxOffset = offsetNumber;
            argumentIndices.add(argumentNumber);

            // now get the format
            I18nConversionCategory category = null;
            if (segments[SEG_TYPE].length() != 0) {
                int type = findKeyword(segments[SEG_TYPE], TYPE_KEYWORDS);
                switch (type) {
                    case TYPE_NULL:
                        category = I18nConversionCategory.GENERAL;
                        break;
                    case TYPE_NUMBER:
                        switch (findKeyword(segments[SEG_MODIFIER], NUMBER_MODIFIER_KEYWORDS)) {
                            case MODIFIER_DEFAULT:
                            case MODIFIER_CURRENCY:
                            case MODIFIER_PERCENT:
                            case MODIFIER_INTEGER:
                                break;
                            default: // DecimalFormat pattern
                                try {
                                    new DecimalFormat(
                                            segments[SEG_MODIFIER],
                                            DecimalFormatSymbols.getInstance(locale));
                                } catch (IllegalArgumentException e) {
                                    maxOffset = oldMaxOffset;
                                    // invalid decimal subformat pattern
                                    throw e;
                                }
                                break;
                        }
                        category = I18nConversionCategory.NUMBER;
                        break;
                    case TYPE_DATE:
                    case TYPE_TIME:
                        int mod = findKeyword(segments[SEG_MODIFIER], DATE_TIME_MODIFIER_KEYWORDS);
                        if (mod >= 0 && mod < DATE_TIME_MODIFIER_KEYWORDS.length) {
                            // nothing to do
                        } else {
                            // SimpleDateFormat pattern
                            try {
                                new SimpleDateFormat(segments[SEG_MODIFIER], locale);
                            } catch (IllegalArgumentException e) {
                                maxOffset = oldMaxOffset;
                                // invalid date subformat pattern
                                throw e;
                            }
                        }
                        category = I18nConversionCategory.DATE;
                        break;
                    case TYPE_CHOICE:
                        if (segments[SEG_MODIFIER].length() == 0) {
                            throw new IllegalArgumentException(
                                    "Choice Pattern requires Subformat Pattern: "
                                            + segments[SEG_MODIFIER]);
                        }
                        try {
                            // ChoiceFormat pattern
                            new ChoiceFormat(segments[SEG_MODIFIER]);
                        } catch (Exception e) {
                            maxOffset = oldMaxOffset;
                            // invalid choice subformat pattern
                            throw new IllegalArgumentException(
                                    "Choice Pattern incorrect: " + segments[SEG_MODIFIER], e);
                        }
                        category = I18nConversionCategory.NUMBER;
                        break;
                    default:
                        maxOffset = oldMaxOffset;
                        throw new IllegalArgumentException(
                                "unknown format type: " + segments[SEG_TYPE]);
                }
            } else {
                category = I18nConversionCategory.GENERAL;
            }
            categories.add(category);
        }

        /**
         * Return the index of s in list. If not found, return the index of
         * s.trim().toLowerCase(Locale.ROOT) in list. If still not found, return -1.
         */
        private static final int findKeyword(String s, String[] list) {
            for (int i = 0; i < list.length; ++i) {
                if (s.equals(list[i])) {
                    return i;
                }
            }

            // Try trimmed lowercase.
            String ls = s.trim().toLowerCase(Locale.ROOT);
            if (ls != s) {
                for (int i = 0; i < list.length; ++i) {
                    if (ls.equals(list[i])) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
}
