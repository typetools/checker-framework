package org.checkerframework.checker.formatter.qual;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.qual.AnnotatedFor;

/**
 * Elements of this enumeration are used in a {@link Format Format} annotation to indicate the valid
 * types that may be passed as a format parameter. For example:
 *
 * <blockquote>
 *
 * <pre>{@literal @}Format({GENERAL, INT}) String f = "String '%s' has length %d";
 *
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
 */
@SuppressWarnings("unchecked") // ".class" expressions in varargs position
@AnnotatedFor("nullness")
public enum ConversionCategory {
  /** Use if the parameter can be of any type. Applicable for conversions b, B, h, H, s, S. */
  GENERAL("bBhHsS", (Class<?>[]) null /* everything */),

  /**
   * Use if the parameter is of a basic types which represent Unicode characters: char, Character,
   * byte, Byte, short, and Short. This conversion may also be applied to the types int and Integer
   * when Character.isValidCodePoint(int) returns true. Applicable for conversions c, C.
   */
  CHAR("cC", Character.class, Byte.class, Short.class, Integer.class),

  /**
   * Use if the parameter is an integral type: byte, Byte, short, Short, int and Integer, long,
   * Long, and BigInteger. Applicable for conversions d, o, x, X.
   */
  INT("doxX", Byte.class, Short.class, Integer.class, Long.class, BigInteger.class),

  /**
   * Use if the parameter is a floating-point type: float, Float, double, Double, and BigDecimal.
   * Applicable for conversions e, E, f, g, G, a, A.
   */
  FLOAT("eEfgGaA", Float.class, Double.class, BigDecimal.class),

  /**
   * Use if the parameter is a type which is capable of encoding a date or time: long, Long,
   * Calendar, and Date. Applicable for conversions t, T.
   */
  @SuppressWarnings("JdkObsolete")
  TIME("tT", Long.class, Calendar.class, Date.class),

  /**
   * Use if the parameter is both a char and an int.
   *
   * <p>In a format string, multiple conversions may be applied to the same parameter. This is
   * seldom needed, but the following is an example of such use:
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
   * <p>All other intersection either lead to an already existing type, or NULL, in which case it is
   * illegal to pass object's of any type as parameter.
   */
  CHAR_AND_INT(null, Byte.class, Short.class, Integer.class),

  /**
   * Use if the parameter is both an int and a time.
   *
   * @see #CHAR_AND_INT
   */
  INT_AND_TIME(null, Long.class),

  /**
   * Use if no object of any type can be passed as parameter. In this case, the only legal value is
   * null. This is seldomly needed, and indicates an error in most cases. For example:
   *
   * <pre>
   *   format("Test %1$f %1$d", null);
   * </pre>
   *
   * Only null can be legally passed, passing a value such as 4 or 4.2 would lead to an exception.
   */
  NULL(null),

  /**
   * Use if a parameter is not used by the formatter. This is seldomly needed, and indicates an
   * error in most cases. For example:
   *
   * <pre>
   *   format("Test %1$s %3$s", "a","unused","b");
   * </pre>
   *
   * Only the first "a" and third "b" parameters are used, the second "unused" parameter is ignored.
   */
  UNUSED(null, (Class<?>[]) null /* everything */);

  /** The argument types. Null means every type. */
  @SuppressWarnings("ImmutableEnumChecker") // TODO: clean this up!
  public final Class<?> @Nullable [] types;

  /** The format specifier characters. Null means users cannot specify it directly. */
  public final @Nullable String chars;

  /**
   * Create a new conversion category.
   *
   * @param chars the format specifier characters. Null means users cannot specify it directly.
   * @param types the argument types. Null means every type.
   */
  ConversionCategory(@Nullable String chars, Class<?> @Nullable ... types) {
    this.chars = chars;
    if (types == null) {
      this.types = types;
    } else {
      List<Class<?>> typesWithPrimitives = new ArrayList<>(types.length);
      for (Class<?> type : types) {
        typesWithPrimitives.add(type);
        Class<?> unwrapped = unwrapPrimitive(type);
        if (unwrapped != null) {
          typesWithPrimitives.add(unwrapped);
        }
      }
      this.types = typesWithPrimitives.toArray(new Class<?>[0]);
    }
  }

  /**
   * If the given class is a primitive wrapper, return the corresponding primitive class. Otherwise
   * return null.
   *
   * @param c a class
   * @return the unwrapped primitive, or null
   */
  private static @Nullable Class<? extends Object> unwrapPrimitive(Class<?> c) {
    if (c == Byte.class) {
      return byte.class;
    }
    if (c == Character.class) {
      return char.class;
    }
    if (c == Short.class) {
      return short.class;
    }
    if (c == Integer.class) {
      return int.class;
    }
    if (c == Long.class) {
      return long.class;
    }
    if (c == Float.class) {
      return float.class;
    }
    if (c == Double.class) {
      return double.class;
    }
    if (c == Boolean.class) {
      return boolean.class;
    }
    return null;
  }

  /**
   * The conversion categories that have a corresponding conversion character. This lacks UNUSED,
   * TIME_AND_INT, etc.
   */
  private static final ConversionCategory[] conversionCategoriesWithChar =
      new ConversionCategory[] {GENERAL, CHAR, INT, FLOAT, TIME};

  /**
   * Converts a conversion character to a category. For example:
   *
   * <pre>{@code
   * ConversionCategory.fromConversionChar('d') == ConversionCategory.INT
   * }</pre>
   *
   * @param c a conversion character
   * @return the category for the given conversion character
   */
  @SuppressWarnings("nullness:dereference.of.nullable") // `chars` field is non-null for these
  public static ConversionCategory fromConversionChar(char c) {
    for (ConversionCategory v : conversionCategoriesWithChar) {
      if (v.chars.contains(String.valueOf(c))) {
        return v;
      }
    }
    throw new IllegalArgumentException("Bad conversion character " + c);
  }

  private static <E> Set<E> arrayToSet(E[] a) {
    return new HashSet<>(Arrays.asList(a));
  }

  public static boolean isSubsetOf(ConversionCategory a, ConversionCategory b) {
    return intersect(a, b) == a;
  }

  /** Conversion categories that need to be considered by {@link #intersect}. */
  private static final ConversionCategory[] conversionCategoriesForIntersect =
      new ConversionCategory[] {CHAR, INT, FLOAT, TIME, CHAR_AND_INT, INT_AND_TIME, NULL};

  /**
   * Returns the intersection of two categories. This is seldomly needed.
   *
   * <blockquote>
   *
   * <pre>
   * ConversionCategory.intersect(INT, TIME) == INT_AND_TIME;
   * </pre>
   *
   * </blockquote>
   *
   * @param a a category
   * @param b a category
   * @return the intersection of the two categories (their greatest lower bound)
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

    @SuppressWarnings("nullness:argument" // `types` field is null only for UNUSED and GENERAL
    )
    Set<Class<?>> as = arrayToSet(a.types);
    @SuppressWarnings("nullness:argument" // `types` field is null only for UNUSED and GENERAL
    )
    Set<Class<?>> bs = arrayToSet(b.types);
    as.retainAll(bs); // intersection
    for (ConversionCategory v : conversionCategoriesForIntersect) {
      @SuppressWarnings("nullness:argument" // `types` field is null only for UNUSED and GENERAL
      )
      Set<Class<?>> vs = arrayToSet(v.types);
      if (vs.equals(as)) {
        return v;
      }
    }
    throw new RuntimeException();
  }

  /** Conversion categories that need to be considered by {@link #union}. */
  private static final ConversionCategory[] conversionCategoriesForUnion =
      new ConversionCategory[] {NULL, CHAR_AND_INT, INT_AND_TIME, CHAR, INT, FLOAT, TIME};

  /**
   * Returns the union of two categories. This is seldomly needed.
   *
   * <blockquote>
   *
   * <pre>
   * ConversionCategory.union(INT, TIME) == GENERAL;
   * </pre>
   *
   * </blockquote>
   *
   * @param a a category
   * @param b a category
   * @return the union of the two categories (their least upper bound)
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

    @SuppressWarnings("nullness:argument" // `types` field is null only for UNUSED and GENERAL
    )
    Set<Class<?>> as = arrayToSet(a.types);
    @SuppressWarnings("nullness:argument" // `types` field is null only for UNUSED and GENERAL
    )
    Set<Class<?>> bs = arrayToSet(b.types);
    as.addAll(bs); // union
    for (ConversionCategory v : conversionCategoriesForUnion) {
      @SuppressWarnings("nullness:argument" // `types` field is null only for UNUSED and GENERAL
      )
      Set<Class<?>> vs = arrayToSet(v.types);
      if (vs.equals(as)) {
        return v;
      }
    }

    return GENERAL;
  }

  /**
   * Returns true if {@code argType} can be an argument used by this format specifier.
   *
   * @param argType an argument type
   * @return true if {@code argType} can be an argument used by this format specifier
   */
  public boolean isAssignableFrom(Class<?> argType) {
    if (types == null) {
      return true;
    }
    if (argType == void.class) {
      return true;
    }
    for (Class<?> c : types) {
      if (c.isAssignableFrom(argType)) {
        return true;
      }
    }
    return false;
  }

  /** Returns a pretty printed {@link ConversionCategory}. */
  @Pure
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(name());
    sb.append(" conversion category");

    if (types == null || types.length == 0) {
      return sb.toString();
    }

    StringJoiner sj = new StringJoiner(", ", "(one of: ", ")");
    for (Class<?> cls : types) {
      sj.add(cls.getSimpleName());
    }
    sb.append(" ");
    sb.append(sj);

    return sb.toString();
  }
}
