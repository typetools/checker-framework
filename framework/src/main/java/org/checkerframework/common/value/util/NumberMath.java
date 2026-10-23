package org.checkerframework.common.value.util;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Performs Java numeric operations on a boxed number. */
public abstract class NumberMath<T extends Number> {

  /** The left operand. */
  protected final T number;

  /**
   * Creates a NumberMath.
   *
   * @param number the left operand
   */
  protected NumberMath(T number) {
    this.number = number;
  }

  /**
   * Returns an arithmetic helper for {@code number}.
   *
   * @param number a number
   * @return an arithmetic helper, or null if {@code number} is not a supported boxed primitive
   */
  public static @Nullable NumberMath<?> getNumberMath(Number number) {
    if (number instanceof Byte) {
      return new ByteMath(number.byteValue());
    }
    if (number instanceof Double) {
      return new DoubleMath(number.doubleValue());
    }
    if (number instanceof Float) {
      return new FloatMath(number.floatValue());
    }
    if (number instanceof Integer) {
      return new IntegerMath(number.intValue());
    }
    if (number instanceof Long) {
      return new LongMath(number.longValue());
    }
    if (number instanceof Short) {
      return new ShortMath(number.shortValue());
    }
    return null;
  }

  /** Returns {@code this + right}. */
  public Number plus(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() + right.doubleValue();
      case FLOAT -> number.floatValue() + right.floatValue();
      case LONG -> number.longValue() + right.longValue();
      case INT -> number.intValue() + right.intValue();
    };
  }

  /** Returns {@code this - right}. */
  public Number minus(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() - right.doubleValue();
      case FLOAT -> number.floatValue() - right.floatValue();
      case LONG -> number.longValue() - right.longValue();
      case INT -> number.intValue() - right.intValue();
    };
  }

  /** Returns {@code this * right}. */
  public Number times(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() * right.doubleValue();
      case FLOAT -> number.floatValue() * right.floatValue();
      case LONG -> number.longValue() * right.longValue();
      case INT -> number.intValue() * right.intValue();
    };
  }

  /**
   * Returns {@code this / right}, or null if {@code right} is zero and the operation is integer
   * division.
   */
  public @Nullable Number divide(Number right) {
    NumericType promotion = binaryPromotion(right);
    if ((promotion == NumericType.INT || promotion == NumericType.LONG) && isIntegralZero(right)) {
      return null;
    }
    return switch (promotion) {
      case DOUBLE -> number.doubleValue() / right.doubleValue();
      case FLOAT -> number.floatValue() / right.floatValue();
      case LONG -> number.longValue() / right.longValue();
      case INT -> number.intValue() / right.intValue();
    };
  }

  /**
   * Returns {@code this % right}, or null if {@code right} is zero and the operation is integer
   * remainder.
   */
  public @Nullable Number remainder(Number right) {
    NumericType promotion = binaryPromotion(right);
    if ((promotion == NumericType.INT || promotion == NumericType.LONG) && isIntegralZero(right)) {
      return null;
    }
    return switch (promotion) {
      case DOUBLE -> number.doubleValue() % right.doubleValue();
      case FLOAT -> number.floatValue() % right.floatValue();
      case LONG -> number.longValue() % right.longValue();
      case INT -> number.intValue() % right.intValue();
    };
  }

  /** Returns {@code this << right}. */
  public Number shiftLeft(Number right) {
    requireIntegral(right);
    if (isLong(number)) {
      return number.longValue() << right.longValue();
    }
    return number.intValue() << right.longValue();
  }

  /** Returns {@code this >> right}. */
  public Number signedShiftRight(Number right) {
    requireIntegral(right);
    if (isLong(number)) {
      return number.longValue() >> right.longValue();
    }
    return number.intValue() >> right.longValue();
  }

  /** Returns {@code this >>> right}. */
  public Number unsignedShiftRight(Number right) {
    requireIntegral(right);
    if (isLong(number)) {
      return number.longValue() >>> right.longValue();
    }
    return number.intValue() >>> right.longValue();
  }

  /** Returns {@code this & right}. */
  public Number bitwiseAnd(Number right) {
    requireIntegral(number);
    requireIntegral(right);
    if (isLong(number) || isLong(right)) {
      return number.longValue() & right.longValue();
    }
    return number.intValue() & right.intValue();
  }

  /** Returns {@code this | right}. */
  public Number bitwiseOr(Number right) {
    requireIntegral(number);
    requireIntegral(right);
    if (isLong(number) || isLong(right)) {
      return number.longValue() | right.longValue();
    }
    return number.intValue() | right.intValue();
  }

  /** Returns {@code this ^ right}. */
  public Number bitwiseXor(Number right) {
    requireIntegral(number);
    requireIntegral(right);
    if (isLong(number) || isLong(right)) {
      return number.longValue() ^ right.longValue();
    }
    return number.intValue() ^ right.intValue();
  }

  /** Returns {@code +this}. */
  public Number unaryPlus() {
    return switch (numericType(number)) {
      case DOUBLE -> +number.doubleValue();
      case FLOAT -> +number.floatValue();
      case LONG -> +number.longValue();
      case INT -> +number.intValue();
    };
  }

  /** Returns {@code -this}. */
  public Number unaryMinus() {
    return switch (numericType(number)) {
      case DOUBLE -> -number.doubleValue();
      case FLOAT -> -number.floatValue();
      case LONG -> -number.longValue();
      case INT -> -number.intValue();
    };
  }

  /** Returns {@code ~this}. */
  public Number bitwiseComplement() {
    requireIntegral(number);
    if (isLong(number)) {
      return ~number.longValue();
    }
    return ~number.intValue();
  }

  /** Returns {@code this == right}. */
  public Boolean equalTo(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() == right.doubleValue();
      case FLOAT -> number.floatValue() == right.floatValue();
      case LONG -> number.longValue() == right.longValue();
      case INT -> number.intValue() == right.intValue();
    };
  }

  /** Returns {@code this != right}. */
  public Boolean notEqualTo(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() != right.doubleValue();
      case FLOAT -> number.floatValue() != right.floatValue();
      case LONG -> number.longValue() != right.longValue();
      case INT -> number.intValue() != right.intValue();
    };
  }

  /** Returns {@code this > right}. */
  public Boolean greaterThan(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() > right.doubleValue();
      case FLOAT -> number.floatValue() > right.floatValue();
      case LONG -> number.longValue() > right.longValue();
      case INT -> number.intValue() > right.intValue();
    };
  }

  /** Returns {@code this >= right}. */
  public Boolean greaterThanEq(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() >= right.doubleValue();
      case FLOAT -> number.floatValue() >= right.floatValue();
      case LONG -> number.longValue() >= right.longValue();
      case INT -> number.intValue() >= right.intValue();
    };
  }

  /** Returns {@code this < right}. */
  public Boolean lessThan(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() < right.doubleValue();
      case FLOAT -> number.floatValue() < right.floatValue();
      case LONG -> number.longValue() < right.longValue();
      case INT -> number.intValue() < right.intValue();
    };
  }

  /** Returns {@code this <= right}. */
  public Boolean lessThanEq(Number right) {
    return switch (binaryPromotion(right)) {
      case DOUBLE -> number.doubleValue() <= right.doubleValue();
      case FLOAT -> number.floatValue() <= right.floatValue();
      case LONG -> number.longValue() <= right.longValue();
      case INT -> number.intValue() <= right.intValue();
    };
  }

  /**
   * Returns whether {@code number} is an integral zero.
   *
   * @param number a number
   * @return whether {@code number} is an integral zero
   */
  public static boolean isIntegralZero(Number number) {
    return isIntegral(number) && number.longValue() == 0;
  }

  /** Returns the binary numeric promotion type for the operands. */
  private NumericType binaryPromotion(Number right) {
    NumericType leftType = numericType(number);
    NumericType rightType = numericType(right);
    if (leftType == NumericType.DOUBLE || rightType == NumericType.DOUBLE) {
      return NumericType.DOUBLE;
    } else if (leftType == NumericType.FLOAT || rightType == NumericType.FLOAT) {
      return NumericType.FLOAT;
    } else if (leftType == NumericType.LONG || rightType == NumericType.LONG) {
      return NumericType.LONG;
    }
    return NumericType.INT;
  }

  /** Returns the numeric promotion type for {@code value}. */
  private static NumericType numericType(Number value) {
    if (value instanceof Double) {
      return NumericType.DOUBLE;
    } else if (value instanceof Float) {
      return NumericType.FLOAT;
    } else if (value instanceof Long) {
      return NumericType.LONG;
    } else if (value instanceof Byte || value instanceof Short || value instanceof Integer) {
      return NumericType.INT;
    }
    throw new UnsupportedOperationException("Unsupported number type: " + value.getClass());
  }

  /** Throws an exception if {@code value} is not an integral boxed primitive. */
  private static void requireIntegral(Number value) {
    if (!isIntegral(value)) {
      throw new UnsupportedOperationException();
    }
  }

  /** Returns whether {@code value} is an integral boxed primitive. */
  private static boolean isIntegral(Number value) {
    return value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long;
  }

  /** Returns whether {@code value} is a boxed long. */
  private static boolean isLong(Number value) {
    return value instanceof Long;
  }

  /** A type produced by Java unary or binary numeric promotion. */
  private enum NumericType {
    /** An int. */
    INT,

    /** A long. */
    LONG,

    /** A float. */
    FLOAT,

    /** A double. */
    DOUBLE
  }
}
