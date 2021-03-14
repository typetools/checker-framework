package org.checkerframework.javacutil;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A utility class that helps with {@link TypeKind}s. */
public final class TypeKindUtils {

  /** This class cannot be instantiated. */
  private TypeKindUtils() {
    throw new AssertionError("Class TypeKindUtils cannot be instantiated.");
  }

  /**
   * Return true if the argument is one of INT, SHORT, BYTE, CHAR, LONG.
   *
   * @param typeKind the TypeKind to inspect
   * @return true if typeKind is a primitive integral type kind
   */
  public static boolean isIntegral(TypeKind typeKind) {
    switch (typeKind) {
      case INT:
      case SHORT:
      case BYTE:
      case CHAR:
      case LONG:
        return true;
      default:
        return false;
    }
  }

  /**
   * Return true if the argument is one of FLOAT, DOUBLE.
   *
   * @param typeKind the TypeKind to inspect
   * @return true if typeKind is a primitive floating point type kind
   */
  public static boolean isFloatingPoint(TypeKind typeKind) {
    switch (typeKind) {
      case FLOAT:
      case DOUBLE:
        return true;
      default:
        return false;
    }
  }

  /**
   * Returns true iff the argument is a primitive numeric type kind.
   *
   * @param typeKind a type kind
   * @return true if the argument is a primitive numeric type kind
   */
  public static boolean isNumeric(TypeKind typeKind) {
    switch (typeKind) {
      case BYTE:
      case CHAR:
      case DOUBLE:
      case FLOAT:
      case INT:
      case LONG:
      case SHORT:
        return true;
      default:
        return false;
    }
  }

  // Cannot create an overload that takes an AnnotatedTypeMirror because the javacutil
  // package must not depend on the framework package.
  /**
   * Given a primitive type, return its kind. Given a boxed primitive type, return the corresponding
   * primitive type kind. Otherwise, return null.
   *
   * @param type a primitive or boxed primitive type
   * @return a primitive type kind, or null
   */
  public static @Nullable TypeKind primitiveOrBoxedToTypeKind(TypeMirror type) {
    final TypeKind typeKind = type.getKind();
    if (typeKind.isPrimitive()) {
      return typeKind;
    }

    if (!(type instanceof DeclaredType)) {
      return null;
    }

    final String typeString = TypesUtils.getQualifiedName((DeclaredType) type).toString();

    switch (typeString) {
      case "java.lang.Byte":
        return TypeKind.BYTE;
      case "java.lang.Boolean":
        return TypeKind.BOOLEAN;
      case "java.lang.Character":
        return TypeKind.CHAR;
      case "java.lang.Double":
        return TypeKind.DOUBLE;
      case "java.lang.Float":
        return TypeKind.FLOAT;
      case "java.lang.Integer":
        return TypeKind.INT;
      case "java.lang.Long":
        return TypeKind.LONG;
      case "java.lang.Short":
        return TypeKind.SHORT;
      default:
        return null;
    }
  }

  // No overload that takes AnnotatedTypeMirror becasue javacutil cannot depend on framework.
  /**
   * Returns the widened numeric type for an arithmetic operation performed on a value of the left
   * type and the right type. Defined in JLS 5.6.2. We return a {@link TypeKind} because creating a
   * {@link TypeMirror} requires a {@link javax.lang.model.util.Types} object from the {@link
   * javax.annotation.processing.ProcessingEnvironment}.
   *
   * @param left a type mirror
   * @param right a type mirror
   * @return the result of widening numeric conversion, or NONE when the conversion cannot be
   *     performed
   */
  public static TypeKind widenedNumericType(TypeMirror left, TypeMirror right) {
    return widenedNumericType(left.getKind(), right.getKind());
  }

  /**
   * Given two type kinds, return the type kind they are widened to, when an arithmetic operation is
   * performed on them. Defined in JLS 5.6.2.
   *
   * @param a a type kind
   * @param b a type kind
   * @return the type kind to which they are widened, when an operation is performed on them
   */
  public static TypeKind widenedNumericType(TypeKind a, TypeKind b) {
    if (!isNumeric(a) || !isNumeric(b)) {
      return TypeKind.NONE;
    }

    if (a == TypeKind.DOUBLE || b == TypeKind.DOUBLE) {
      return TypeKind.DOUBLE;
    }

    if (a == TypeKind.FLOAT || b == TypeKind.FLOAT) {
      return TypeKind.FLOAT;
    }

    if (a == TypeKind.LONG || b == TypeKind.LONG) {
      return TypeKind.LONG;
    }

    return TypeKind.INT;
  }

  /** The type of primitive conversion: narrowing, widening, or same. */
  public enum PrimitiveConversionKind {
    /** The two primitive kinds are the same. */
    SAME,
    /**
     * The conversion is a widening primitive conversion.
     *
     * <p>This includes byte to char, even though that is strictly a "widening and narrowing
     * primitive conversion", according to JLS 5.1.4.
     */
    WIDENING,
    /** The conversion is a narrowing primitive conversion. */
    NARROWING
  }

  /**
   * Return the type of primitive conversion between {@code from} and {@code to}.
   *
   * <p>The narrowing conversions include both short to char and char to short.
   *
   * @param from a primitive type
   * @param to a primitive type
   * @return the type of primitive conversion between {@code from} and {@code to}
   */
  public static PrimitiveConversionKind getPrimitiveConversionKind(TypeKind from, TypeKind to) {
    if (from == TypeKind.BOOLEAN && to == TypeKind.BOOLEAN) {
      return PrimitiveConversionKind.SAME;
    }

    assert (isIntegral(from) || isFloatingPoint(from)) && (isIntegral(to) || isFloatingPoint(to))
        : "getPrimitiveConversionKind " + from + " " + to;

    if (from == to) {
      return PrimitiveConversionKind.SAME;
    }

    boolean fromIntegral = isIntegral(from);
    boolean toFloatingPoint = isFloatingPoint(to);
    if (fromIntegral && toFloatingPoint) {
      return PrimitiveConversionKind.WIDENING;
    }

    boolean toIntegral = isIntegral(to);
    boolean fromFloatingPoint = isFloatingPoint(from);
    if (fromFloatingPoint && toIntegral) {
      return PrimitiveConversionKind.NARROWING;
    }

    if (numBits(from) < numBits(to)) {
      return PrimitiveConversionKind.WIDENING;
    } else {
      // If same number of bits (char to short or short to char), it is a narrowing.
      return PrimitiveConversionKind.NARROWING;
    }
  }

  /**
   * Returns the number of bits in the representation of a primitive type. Returns -1 if the type is
   * not a primitive type.
   *
   * @param tk a primitive type kind
   * @return the number of bits in its representation, or -1 if not integral
   */
  private static int numBits(TypeKind tk) {
    switch (tk) {
      case BYTE:
        return 8;
      case SHORT:
        return 16;
      case CHAR:
        return 16;
      case INT:
        return 32;
      case LONG:
        return 64;
      case FLOAT:
        return 32;
      case DOUBLE:
        return 64;
      case BOOLEAN:
      default:
        return -1;
    }
  }

  /**
   * Returns the minimum value representable by the given ingetgral primitive type.
   *
   * @param tk a primitive type kind
   * @return the minimum value representable by the given integral primitive type.
   */
  public static long minValue(TypeKind tk) {
    switch (tk) {
      case BYTE:
        return Byte.MIN_VALUE;
      case SHORT:
        return Short.MIN_VALUE;
      case CHAR:
        return Character.MIN_VALUE;
      case INT:
        return Integer.MIN_VALUE;
      case LONG:
        return Long.MIN_VALUE;
      case FLOAT:
      case DOUBLE:
      case BOOLEAN:
      default:
        throw new BugInCF(tk + " does not have a minimum value");
    }
  }

  /**
   * Returns the maximum value representable by the given integral primitive type.
   *
   * @param tk a primitive type kind
   * @return the maximum value representable by the given integral primitive type.
   */
  public static long maxValue(TypeKind tk) {
    switch (tk) {
      case BYTE:
        return Byte.MAX_VALUE;
      case SHORT:
        return Short.MAX_VALUE;
      case CHAR:
        return Character.MAX_VALUE;
      case INT:
        return Integer.MAX_VALUE;
      case LONG:
        return Long.MAX_VALUE;
      case FLOAT:
      case DOUBLE:
      case BOOLEAN:
      default:
        throw new BugInCF(tk + " does not have a maximum value");
    }
  }
}
