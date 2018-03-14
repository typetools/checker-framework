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

/** This class provides a collection of utilities to ease working with format strings. */
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
            conv.put(
                    last,
                    ConversionCategory.intersect(
                            conv.containsKey(last) ? conv.get(last) : ConversionCategory.UNUSED,
                            c.category()));
        }

        ConversionCategory[] res = new ConversionCategory[maxindex + 1];
        for (int i = 0; i <= maxindex; ++i) {
            res[i] = conv.containsKey(i) ? conv.get(i) : ConversionCategory.UNUSED;
        }
        return res;
    }

    // %[argument_index$][flags][width][.precision][t]conversion
    private static final String formatSpecifier =
            "%(\\d+\\$)?([-#+ 0,(\\<]*)?(\\d+)?(\\.\\d+)?([tT])?([a-zA-Z%])";

    private static Pattern fsPattern = Pattern.compile(formatSpecifier);

    private static int indexFromFormat(Matcher m) {
        int index;
        String s = m.group(1);
        if (s != null) { // explicit index
            index = Integer.parseInt(s.substring(0, s.length() - 1));
        } else {
            if (m.group(2) != null && m.group(2).contains(String.valueOf('<'))) {
                index = -1; // relative index
            } else {
                index = 0; // ordinary index
            }
        }
        return index;
    }

    private static char conversionCharFromFormat(Matcher m) {
        String dt = m.group(5);
        if (dt == null) {
            return m.group(6).charAt(0);
        } else {
            return dt.charAt(0);
        }
    }

    private static Conversion[] parse(String format) {
        ArrayList<Conversion> cs = new ArrayList<>();
        Matcher m = fsPattern.matcher(format);
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
                    expected.chars.length() == 0 ? '-' : expected.chars.charAt(0),
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
