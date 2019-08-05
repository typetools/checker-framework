package org.checkerframework.checker.i18nformatter.qual;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Elements of this enumeration are used in a {@link I18nFormat} annotation to indicate the valid
 * types that may be passed as a format parameter. For example:
 *
 * <blockquote>
 *
 * <pre>{@literal @}I18nFormat({I18nConversionCategory.GENERAL, I18nConversionCategory.NUMBER})
 * String f = "{0}{1, number}";
 * MessageFormat.format(f, "Example", 0) // valid</pre>
 *
 * </blockquote>
 *
 * The annotation indicates that the format string requires any object as the first parameter
 * ({@link I18nConversionCategory#GENERAL}) and a number as the second parameter ({@link
 * I18nConversionCategory#NUMBER}).
 *
 * @checker_framework.manual #i18n-formatter-checker Internationalization Format String Checker
 */
public enum I18nConversionCategory {

    /**
     * Use if a parameter is not used by the formatter.
     *
     * <pre>
     * MessageFormat.format(&quot;{1}&quot;, a, b);
     * </pre>
     *
     * Only the second argument ("b") is used. The first argument ("a") is ignored
     */
    UNUSED(null /* everything */, null),

    /** Use if the parameter can be of any type. */
    GENERAL(null /* everything */, null),

    /** Use if the parameter can be of date, time, or number types. */
    DATE(new Class<?>[] {Date.class, Number.class}, new String[] {"date", "time"}),

    /**
     * Use if the parameter can be of number or choice types. An example of choice:
     *
     * <pre>{@code
     * format("{0, choice, 0#zero|1#one|1<{0, number} is more than 1}", 2)
     * }</pre>
     *
     * This will print "2 is more than 1".
     */
    NUMBER(new Class<?>[] {Number.class}, new String[] {"number", "choice"});

    @SuppressWarnings("ImmutableEnumChecker") // TODO: clean this up!
    public final Class<? extends Object>[] types;

    @SuppressWarnings("ImmutableEnumChecker") // TODO: clean this up!
    public final String[] strings;

    I18nConversionCategory(Class<? extends Object>[] types, String[] strings) {
        this.types = types;
        this.strings = strings;
    }

    /** Used by {@link #stringToI18nConversionCategory}. */
    static I18nConversionCategory[] namedCategories = new I18nConversionCategory[] {DATE, NUMBER};

    /**
     * Creates a conversion cagetogry from a string name.
     *
     * <pre>
     * I18nConversionCategory.stringToI18nConversionCategory("number") == I18nConversionCategory.NUMBER;
     * </pre>
     *
     * @return the I18nConversionCategory associated with the given string
     */
    public static I18nConversionCategory stringToI18nConversionCategory(String string) {
        string = string.toLowerCase();
        for (I18nConversionCategory v : namedCategories) {
            for (String s : v.strings) {
                if (s.equals(string)) {
                    return v;
                }
            }
        }
        throw new IllegalArgumentException("Invalid format type " + string);
    }

    private static <E> Set<E> arrayToSet(E[] a) {
        return new HashSet<>(Arrays.asList(a));
    }

    /** @return true if a is a subset of b */
    public static boolean isSubsetOf(I18nConversionCategory a, I18nConversionCategory b) {
        return intersect(a, b) == a;
    }

    /**
     * Returns the intersection of the two given I18nConversionCategories.
     *
     * <blockquote>
     *
     * <pre>
     * I18nConversionCategory.intersect(DATE, NUMBER) == NUMBER;
     * </pre>
     *
     * </blockquote>
     */
    public static I18nConversionCategory intersect(
            I18nConversionCategory a, I18nConversionCategory b) {
        if (a == UNUSED) {
            return b;
        }
        if (b == UNUSED) {
            return a;
        }
        if (a == GENERAL) {
            return b;
        }
        if (b == GENERAL) {
            return a;
        }

        Set<Class<? extends Object>> as = arrayToSet(a.types);
        Set<Class<? extends Object>> bs = arrayToSet(b.types);
        as.retainAll(bs); // intersection
        for (I18nConversionCategory v : new I18nConversionCategory[] {DATE, NUMBER}) {
            Set<Class<? extends Object>> vs = arrayToSet(v.types);
            if (vs.equals(as)) {
                return v;
            }
        }
        throw new RuntimeException();
    }

    /**
     * Returns the union of the two given I18nConversionCategories.
     *
     * <pre>
     * I18nConversionCategory.intersect(DATE, NUMBER) == DATE;
     * </pre>
     */
    public static I18nConversionCategory union(I18nConversionCategory a, I18nConversionCategory b) {
        if (a == UNUSED || b == UNUSED) {
            return UNUSED;
        }
        if (a == GENERAL || b == GENERAL) {
            return GENERAL;
        }
        if (a == DATE || b == DATE) {
            return DATE;
        }
        return NUMBER;
    }

    /** Returns a pretty printed {@link I18nConversionCategory}. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.name());
        if (this.types == null) {
            sb.append(" conversion category (all types)");
        } else {
            sb.append(" conversion category (one of: ");
            boolean first = true;
            for (Class<? extends Object> cls : this.types) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(cls.getCanonicalName());
                first = false;
            }
            sb.append(")");
        }
        return sb.toString();
    }
}
