package org.checkerframework.checker.formatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.MissingFormatArgumentException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.checkerframework.checker.formatter.qual.ConversionCategory;
import org.checkerframework.checker.formatter.qual.ReturnsFormat;
import org.checkerframework.checker.regex.qual.Regex;

/** This class provides a collection of utilities to ease working with format strings. */
// TODO @AnnotatedFor("nullness")
public class FormatUtil {
    private static class Conversion {
        private final int index;
        private final ConversionCategory cath;

        public Conversion(char c, int index) {
            this.index = index;
            this.cath = ConversionCategory.fromConversionChar(c);
        }

        int index() {
            return index;
        }

        ConversionCategory category() {
            return cath;
        }
    }

    /**
     * Returns if the format string is satisfiable, and if the format's parameters match the passed
     * {@link ConversionCategory}s. Otherwise an {@link Error} is thrown.
     *
     * <p>TODO introduce more such functions, see RegexUtil for examples
     */
    @SuppressWarnings("nullness:argument.type.incompatible") // https://tinyurl.com/cfissue/3449
    @ReturnsFormat
    public static String asFormat(String format, ConversionCategory... cc)
            throws IllegalFormatException {
        ConversionCategory[] fcc = formatParameterCategories(format);
        if (fcc.length != cc.length) {
            throw new ExcessiveOrMissingFormatArgumentException(cc.length, fcc.length);
        }

        for (int i = 0; i < cc.length; i++) {
            if (cc[i] != fcc[i]) {
                throw new IllegalFormatConversionCategoryException(cc[i], fcc[i]);
            }
        }

        return format;
    }

    /** Throws an exception if the format is not syntactically valid. */
    @SuppressWarnings("nullness:argument.type.incompatible") // https://tinyurl.com/cfissue/3449
    public static void tryFormatSatisfiability(String format) throws IllegalFormatException {
        @SuppressWarnings("unused")
        String unused = String.format(format, (Object[]) null);
    }

    /**
     * Returns a {@link ConversionCategory} for every conversion found in the format string.
     *
     * <p>Throws an exception if the format is not syntactically valid.
     */
    public static ConversionCategory[] formatParameterCategories(String format)
            throws IllegalFormatException {
        tryFormatSatisfiability(format);

        int last = -1; // index of last argument referenced
        int lasto = -1; // last ordinary index
        int maxindex = -1;

        Conversion[] cs = parse(format);
        Map<Integer, ConversionCategory> conv = new HashMap<>();

        for (Conversion c : cs) {
            int index = c.index();
            switch (index) {
                case -1: // relative index
                    break;
                case 0: // ordinary index
                    lasto++;
                    last = lasto;
                    break;
                default: // explicit index
                    last = index - 1;
                    break;
            }
            maxindex = Math.max(maxindex, last);
            Integer lastKey = last;
            conv.put(
                    last,
                    ConversionCategory.intersect(
                            conv.containsKey(lastKey)
                                    ? conv.get(lastKey)
                                    : ConversionCategory.UNUSED,
                            c.category()));
        }

        ConversionCategory[] res = new ConversionCategory[maxindex + 1];
        for (int i = 0; i <= maxindex; ++i) {
            Integer key = i; // autoboxing prevents recognizing that containsKey => get() != null
            res[i] = conv.containsKey(key) ? conv.get(key) : ConversionCategory.UNUSED;
        }
        return res;
    }

    // %[argument_index$][flags][width][.precision][t]conversion
    // group 1            2      3      4           5 6
    // For dates and times, the [t] is required and precision must not be provided.
    // For types other than dates and times, the [t] must not be provided.
    // See
    // https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Formatter.html#syntax
    // .
    private static final @Regex(6) String formatSpecifier =
            "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";
    // Groups.  Update if formatSpecifier is updated.
    private static final int formatSpecifierT = 5;
    private static final int formatSpecifierConversion = 6;

    private static @Regex(6) Pattern fsPattern = Pattern.compile(formatSpecifier);

    private static int indexFromFormat(Matcher m) {
        int index;
        String s = m.group(1);
        if (s != null) { // explicit index
            index = Integer.parseInt(s.substring(0, s.length() - 1));
        } else {
            String group2 = m.group(2); // not @Deterministic, so extract into local var
            if (group2 != null && group2.contains(String.valueOf('<'))) {
                index = -1; // relative index
            } else {
                index = 0; // ordinary index
            }
        }
        return index;
    }

    @SuppressWarnings(
            "nullness:dereference.of.nullable") // group formatSpecifierConversion always exists
    private static char conversionCharFromFormat(@Regex(6) Matcher m) {
        String tGroup = m.group(formatSpecifierT);
        if (tGroup != null) {
            return tGroup.charAt(0); // This is the letter "t" or "T".
        } else {
            return m.group(formatSpecifierConversion).charAt(0);
        }
    }

    /**
     * Return the conversion character that is in the given format specifier
     *
     * @param formatSpecifier a format specifier; see {@url
     *     https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/Formatter.html#syntax}
     * @return the conversion character that is in the given format specifier
     * @deprecated This method is public only for testing. Use {@link
     *     #conversionCharFromFormat(Matcher)}.
     */
    @Deprecated // used only for testing.  Use conversionCharFromFormat(Matcher).
    public static char conversionCharFromFormat(String format) {
        Matcher m = fsPattern.matcher(format);
        assert m.find();
        return conversionCharFromFormat(m);
    }

    private static Conversion[] parse(String format) {
        ArrayList<Conversion> cs = new ArrayList<>();
        @Regex(7) Matcher m = fsPattern.matcher(format);
        while (m.find()) {
            char c = conversionCharFromFormat(m);
            switch (c) {
                case '%':
                case 'n':
                    break;
                default:
                    cs.add(new Conversion(c, indexFromFormat(m)));
            }
        }
        return cs.toArray(new Conversion[cs.size()]);
    }

    public static class ExcessiveOrMissingFormatArgumentException
            extends MissingFormatArgumentException {
        private static final long serialVersionUID = 17000126L;

        private final int expected;
        private final int found;

        /**
         * Constructs an instance of this class with the actual argument length and the expected
         * one.
         */
        public ExcessiveOrMissingFormatArgumentException(int expected, int found) {
            super("-");
            this.expected = expected;
            this.found = found;
        }

        public int getExpected() {
            return expected;
        }

        public int getFound() {
            return found;
        }

        @Override
        public String getMessage() {
            return String.format("Expected %d arguments but found %d.", expected, found);
        }
    }

    public static class IllegalFormatConversionCategoryException
            extends IllegalFormatConversionException {
        private static final long serialVersionUID = 17000126L;

        private final ConversionCategory expected;
        private final ConversionCategory found;

        /**
         * Constructs an instance of this class with the mismatched conversion and the expected one.
         */
        public IllegalFormatConversionCategoryException(
                ConversionCategory expected, ConversionCategory found) {
            super(
                    expected.chars == null || expected.chars.length() == 0
                            ? '-'
                            : expected.chars.charAt(0),
                    found.types == null ? Object.class : found.types[0]);
            this.expected = expected;
            this.found = found;
        }

        public ConversionCategory getExpected() {
            return expected;
        }

        public ConversionCategory getFound() {
            return found;
        }

        @Override
        public String getMessage() {
            return String.format("Expected category %s but found %s.", expected, found);
        }
    }
}
