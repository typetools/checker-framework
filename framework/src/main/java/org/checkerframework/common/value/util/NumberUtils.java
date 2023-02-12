package org.checkerframework.common.value.util;

import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.javacutil.TypeKindUtils;
import org.plumelib.util.CollectionsPlume;

/** Utility routines for manipulating numbers. */
public class NumberUtils {

  /**
   * Converts a {@code List<A>} to a {@code List<B>}, where A and B are numeric types.
   *
   * @param type the type to cast to
   * @param numbers the numbers to cast to the given type
   */
  public static List<? extends Number> castNumbers(
      TypeMirror type, List<? extends Number> numbers) {
    return castNumbers(type, false, numbers);
  }

  /**
   * Converts a {@code List<A>} to a {@code List<B>}, where A and B are numeric types.
   *
   * @param type the type to cast to
   * @param isUnsigned if true, treat {@code type} as unsigned
   * @param numbers the numbers to cast to the given type
   */
  @SuppressWarnings("unchecked")
  public static List<? extends Number> castNumbers(
      TypeMirror type, boolean isUnsigned, List<? extends Number> numbers) {
    if (numbers == null) {
      return null;
    }
    TypeKind typeKind = TypeKindUtils.primitiveOrBoxedToTypeKind(type);
    if (typeKind == null) {
      throw new UnsupportedOperationException(type.toString());
    }
    switch (typeKind) {
      case BYTE:
        if (isUnsigned) {
          return CollectionsPlume.<Number, Short>mapList(
              NumberUtils::byteValueUnsigned, (Iterable<Number>) numbers);
        } else {
          return CollectionsPlume.mapList(Number::byteValue, numbers);
        }
      case CHAR:
        return CollectionsPlume.mapList(Number::intValue, numbers);
      case DOUBLE:
        return CollectionsPlume.mapList(Number::doubleValue, numbers);
      case FLOAT:
        return CollectionsPlume.mapList(Number::floatValue, numbers);
      case INT:
        return CollectionsPlume.mapList(Number::intValue, numbers);
      case LONG:
        return CollectionsPlume.mapList(Number::longValue, numbers);
      case SHORT:
        return CollectionsPlume.mapList(Number::shortValue, numbers);
      default:
        throw new UnsupportedOperationException(typeKind + ": " + type);
    }
  }

  /**
   * Returns the given number, casted to unsigned byte.
   *
   * @param n a number
   * @return the given number, casted to unsigned byte
   */
  private static Short byteValueUnsigned(Number n) {
    short result = n.byteValue();
    if (result > 127) {
      result = (short) (result - 256);
    }
    return result;
  }

  /**
   * Return a range that restricts the given range to the given type. That is, return the range
   * resulting from casting a value with the given range.
   *
   * @param type the type for the cast; the result will be within it
   * @param range the original range; the result will be within it
   * @return the intersection of the given range and the possible values of the given type
   */
  public static Range castRange(TypeMirror type, Range range) {
    return castRange(type, false, range);
  }

  /**
   * Return a range that restricts the given range to the given type. That is, return the range
   * resulting from casting a value with the given range.
   *
   * @param type the type for the cast; the result will be within it
   * @param isUnsigned if true, {@code type} should be treated as unsigned
   * @param range the original range; the result will be within it
   * @return the intersection of the given range and the possible values of the given type
   */
  public static Range castRange(TypeMirror type, boolean isUnsigned, Range range) {
    TypeKind typeKind = TypeKindUtils.primitiveOrBoxedToTypeKind(type);
    if (typeKind == null) {
      throw new UnsupportedOperationException(type.toString());
    }
    switch (typeKind) {
      case BYTE:
        return range.byteRange(isUnsigned);
      case CHAR:
        return range.charRange();
      case SHORT:
        return range.shortRange();
      case INT:
        return range.intRange();
      case LONG:
      case FLOAT:
      case DOUBLE:
        return range;
      default:
        throw new UnsupportedOperationException(typeKind + ": " + type);
    }
  }
}
