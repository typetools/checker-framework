package org.checkerframework.checker.formatter.qual;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.checkerframework.dataflow.qual.Pure;

/**
 * Elements of this enumeration are used in a {@link Format Format} annotation to indicate the valid
 * types that may be passed as a format parameter. For example:
 *
 * <blockquote>
 *
 * <pre>{@literal @}Format({ConversionCategory.GENERAL, ConversionCategory.INT})
 * String f = "String '%s' has length %d";
 * String.format(f, "Example", 7);</pre>
 *
 * </blockquote>
 *
 * The annotation indicates that the format string requires any Object as the first parameter
 * ({@link ConversionCategory#GENERAL}) and an integer as the second parameter ({@link
 * ConversionCategory#INT}).
 *
 * @see Format
 * @checker_framework.manual #formatter-checker Format String Checker
 * @author Konstantin Weitz
 */
public enum ConversionCategory {
    /** Use if the parameter can be of any type. Applicable for conversions b, B, h, H, s, S. */
    GENERAL(null /* everything */, "bBhHsS"),

    /**
     * Use if the parameter is of a basic types which represent Unicode characters: char, Character,
     * byte, Byte, short, and Short. This conversion may also be applied to the types int and
     * Integer when Character.isValidCodePoint(int) returns true. Applicable for conversions c, C.
     */
    CHAR(new Class<?>[] {Character.class, Byte.class, Short.class, Integer.class}, "cC"),

    /**
     * Use if the parameter is an integral type: byte, Byte, short, Short, int and Integer, long,
     * Long, and BigInteger. Applicable for conversions d, o, x, X.
     */
    INT(
            new Class<?>[] {Byte.class, Short.class, Integer.class, Long.class, BigInteger.class},
            "doxX"),

    /**
     * Use if the parameter is a floating-point type: float, Float, double, Double, and BigDecimal.
     * Applicable for conversions e, E, f, g, G, a, A.
     */
    FLOAT(new Class<?>[] {Float.class, Double.class, BigDecimal.class}, "eEfgGaA"),

    /**
     * Use if the parameter is a type which is capable of encoding a date or time: long, Long,
     * Calendar, and Date. Applicable for conversions t, T.
     */
    TIME(new Class<?>[] {Long.class, Calendar.class, Date.class}, "tT"),

    /**
     * In a format string, multiple conversions may be applied to the same parameter. This is
     * seldomly needed, but the following is an example of such use:
     *
     * <pre>
     *   format("Test %1$c %1$d", (int)42);
     * </pre>
     *
     * In this example, the first parameter is interpreted as both a character and an int, therefore
     * the parameter must be compatible with both conversion, and can therefore neither be char nor
     * long. This intersection of conversions is called CHAR_AND_INT.
     *
     * <p>One other conversion intersection is interesting, namely the intersection of INT and TIME,
     * resulting in INT_AND_TIME.
     *
     * <p>All other intersection either lead to an already existing type, or NULL, in which case it
     * is illegal to pass object's of any type as parameter.
     */
    CHAR_AND_INT(new Class<?>[] {Byte.class, Short.class, Integer.class}, null),

    INT_AND_TIME(new Class<?>[] {Long.class}, null),

    /**
     * Use if no object of any type can be passed as parameter. In this case, the only legal value
     * is null. This is seldomly needed, and indicates an error in most cases. For example:
     *
     * <pre>
     *   format("Test %1$f %1$d", null);
     * </pre>
     *
     * Only null can be legally passed, passing a value such as 4 or 4.2 would lead to an exception.
     */
    NULL(new Class<?>[0], null),

    /**
     * Use if a parameter is not used by the formatter. This is seldomly needed, and indicates an
     * error in most cases. For example:
     *
     * <pre>
     *   format("Test %1$s %3$s", "a","unused","b");
     * </pre>
     *
     * Only the first "a" and third "b" parameters are used, the second "unused" parameter is
     * ignored.
     */
    UNUSED(null /* everything */, null);

    ConversionCategory(Class<? extends Object>[] types, String chars) {
        this.types = types;
        this.chars = chars;
    }

    @SuppressWarnings("ImmutableEnumChecker") // TODO: clean this up!
    public final Class<? extends Object>[] types;

    public final String chars;

    /**
     * Use this function to get the category associated with a conversion character. For example:
     *
     * <blockquote>
     *
     * <pre>
     * ConversionCategory.fromConversionChar('d') == ConversionCategory.INT;
     * </pre>
     *
     * </blockquote>
     */
    public static ConversionCategory fromConversionChar(char c) {
        for (ConversionCategory v : new ConversionCategory[] {GENERAL, CHAR, INT, FLOAT, TIME}) {
            if (v.chars.contains(String.valueOf(c))) {
                return v;
            }
        }
        throw new IllegalArgumentException();
    }

    private static <E> Set<E> arrayToSet(E[] a) {
        return new HashSet<E>(Arrays.asList(a));
    }

    public static boolean isSubsetOf(ConversionCategory a, ConversionCategory b) {
        return intersect(a, b) == a;
    }

    /**
     * Use this function to get the intersection of two categories. This is seldomly needed.
     *
     * <blockquote>
     *
     * <pre>
     * ConversionCategory.intersect(INT, TIME) == INT_AND_TIME;
     * </pre>
     *
     * </blockquote>
     */
    public static ConversionCategory intersect(ConversionCategory a, ConversionCategory b) {
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
        for (ConversionCategory v :
                new ConversionCategory[] {
                    CHAR, INT, FLOAT, TIME, CHAR_AND_INT, INT_AND_TIME, NULL
                }) {
            Set<Class<? extends Object>> vs = arrayToSet(v.types);
            if (vs.equals(as)) {
                return v;
            }
        }
        // this should never happen
        throw new RuntimeException();
    }

    /**
     * Use this function to get the union of two categories. This is seldomly needed.
     *
     * <blockquote>
     *
     * <pre>
     * ConversionCategory.union(INT, TIME) == GENERAL;
     * </pre>
     *
     * </blockquote>
     */
    public static ConversionCategory union(ConversionCategory a, ConversionCategory b) {
        if (a == UNUSED || b == UNUSED) {
            return UNUSED;
        }
        if (a == GENERAL || b == GENERAL) {
            return GENERAL;
        }
        if ((a == CHAR_AND_INT && b == INT_AND_TIME) || (a == INT_AND_TIME && b == CHAR_AND_INT)) {
            // This is special-cased because the union of a.types and b.types
            // does not include BigInteger.class, whereas the types for INT does.
            // Returning INT here to prevent returning GENERAL below.
            return INT;
        }

        Set<Class<? extends Object>> as = arrayToSet(a.types);
        Set<Class<? extends Object>> bs = arrayToSet(b.types);
        as.addAll(bs); // union
        for (ConversionCategory v :
                new ConversionCategory[] {
                    NULL, CHAR_AND_INT, INT_AND_TIME, CHAR, INT, FLOAT, TIME
                }) {
            Set<Class<? extends Object>> vs = arrayToSet(v.types);
            if (vs.equals(as)) {
                return v;
            }
        }

        return GENERAL;
    }

    private String className(Class<?> cls) {
        if (cls == Boolean.class) {
            return "boolean";
        }
        if (cls == Character.class) {
            return "char";
        }
        if (cls == Byte.class) {
            return "byte";
        }
        if (cls == Short.class) {
            return "short";
        }
        if (cls == Integer.class) {
            return "int";
        }
        if (cls == Long.class) {
            return "long";
        }
        if (cls == Float.class) {
            return "float";
        }
        if (cls == Double.class) {
            return "double";
        }
        return cls.getSimpleName();
    }

    /** Returns a pretty printed {@link ConversionCategory}. */
    @Pure
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(this.name());
        sb.append(" conversion category (one of: ");
        boolean first = true;
        for (Class<? extends Object> cls : this.types) {
            if (!first) {
                sb.append(", ");
            }
            sb.append(className(cls));
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }
}
