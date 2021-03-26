package org.checkerframework.common.value.util;

public class LongMath extends NumberMath<Long> {
  long number;

  public LongMath(long i) {
    number = i;
  }

  @Override
  public Number plus(Number right) {
    if (right instanceof Byte) {
      return number + right.byteValue();
    }
    if (right instanceof Double) {
      return number + right.doubleValue();
    }
    if (right instanceof Float) {
      return number + right.floatValue();
    }
    if (right instanceof Integer) {
      return number + right.intValue();
    }
    if (right instanceof Long) {
      return number + right.longValue();
    }
    if (right instanceof Short) {
      return number + right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number minus(Number right) {
    if (right instanceof Byte) {
      return number - right.byteValue();
    }
    if (right instanceof Double) {
      return number - right.doubleValue();
    }
    if (right instanceof Float) {
      return number - right.floatValue();
    }
    if (right instanceof Integer) {
      return number - right.intValue();
    }
    if (right instanceof Long) {
      return number - right.longValue();
    }
    if (right instanceof Short) {
      return number - right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number times(Number right) {
    if (right instanceof Byte) {
      return number * right.byteValue();
    }
    if (right instanceof Double) {
      return number * right.doubleValue();
    }
    if (right instanceof Float) {
      return number * right.floatValue();
    }
    if (right instanceof Integer) {
      return number * right.intValue();
    }
    if (right instanceof Long) {
      return number * right.longValue();
    }
    if (right instanceof Short) {
      return number * right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number divide(Number right) {
    if (isIntegralZero(right)) {
      return null;
    }
    if (right instanceof Byte) {
      return number / right.byteValue();
    }
    if (right instanceof Double) {
      return number / right.doubleValue();
    }
    if (right instanceof Float) {
      return number / right.floatValue();
    }
    if (right instanceof Integer) {
      return number / right.intValue();
    }
    if (right instanceof Long) {
      return number / right.longValue();
    }
    if (right instanceof Short) {
      return number / right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number remainder(Number right) {
    if (isIntegralZero(right)) {
      return null;
    }
    if (right instanceof Byte) {
      return number % right.byteValue();
    }
    if (right instanceof Double) {
      return number % right.doubleValue();
    }
    if (right instanceof Float) {
      return number % right.floatValue();
    }
    if (right instanceof Integer) {
      return number % right.intValue();
    }
    if (right instanceof Long) {
      return number % right.longValue();
    }
    if (right instanceof Short) {
      return number % right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number shiftLeft(Number right) {
    if (right instanceof Byte) {
      return number << right.byteValue();
    }
    if (right instanceof Integer) {
      return number << right.intValue();
    }
    if (right instanceof Long) {
      return number << right.longValue();
    }
    if (right instanceof Short) {
      return number << right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number signedShiftRight(Number right) {
    if (right instanceof Byte) {
      return number >> right.byteValue();
    }
    if (right instanceof Integer) {
      return number >> right.intValue();
    }
    if (right instanceof Long) {
      return number >> right.longValue();
    }
    if (right instanceof Short) {
      return number >> right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number unsignedShiftRight(Number right) {
    if (right instanceof Byte) {
      return number >>> right.byteValue();
    }
    if (right instanceof Integer) {
      return number >>> right.intValue();
    }
    if (right instanceof Long) {
      return number >>> right.longValue();
    }
    if (right instanceof Short) {
      return number >>> right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number bitwiseAnd(Number right) {
    if (right instanceof Byte) {
      return number & right.byteValue();
    }
    if (right instanceof Integer) {
      return number & right.intValue();
    }
    if (right instanceof Long) {
      return number & right.longValue();
    }
    if (right instanceof Short) {
      return number & right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number bitwiseXor(Number right) {
    if (right instanceof Byte) {
      return number ^ right.byteValue();
    }
    if (right instanceof Integer) {
      return number ^ right.intValue();
    }
    if (right instanceof Long) {
      return number ^ right.longValue();
    }
    if (right instanceof Short) {
      return number ^ right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number bitwiseOr(Number right) {
    if (right instanceof Byte) {
      return number | right.byteValue();
    }
    if (right instanceof Integer) {
      return number | right.intValue();
    }
    if (right instanceof Long) {
      return number | right.longValue();
    }
    if (right instanceof Short) {
      return number | right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Number unaryPlus() {
    return +number;
  }

  @Override
  public Number unaryMinus() {
    return -number;
  }

  @Override
  public Number bitwiseComplement() {
    return ~number;
  }

  @Override
  public Boolean equalTo(Number right) {
    if (right instanceof Byte) {
      return number == right.byteValue();
    }
    if (right instanceof Double) {
      return number == right.doubleValue();
    }
    if (right instanceof Float) {
      return number == right.floatValue();
    }
    if (right instanceof Integer) {
      return number == right.intValue();
    }
    if (right instanceof Long) {
      return number == right.longValue();
    }
    if (right instanceof Short) {
      return number == right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean notEqualTo(Number right) {
    if (right instanceof Byte) {
      return number != right.byteValue();
    }
    if (right instanceof Double) {
      return number != right.doubleValue();
    }
    if (right instanceof Float) {
      return number != right.floatValue();
    }
    if (right instanceof Integer) {
      return number != right.intValue();
    }
    if (right instanceof Long) {
      return number != right.longValue();
    }
    if (right instanceof Short) {
      return number != right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean greaterThan(Number right) {
    if (right instanceof Byte) {
      return number > right.byteValue();
    }
    if (right instanceof Double) {
      return number > right.doubleValue();
    }
    if (right instanceof Float) {
      return number > right.floatValue();
    }
    if (right instanceof Integer) {
      return number > right.intValue();
    }
    if (right instanceof Long) {
      return number > right.longValue();
    }
    if (right instanceof Short) {
      return number > right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean greaterThanEq(Number right) {
    if (right instanceof Byte) {
      return number >= right.byteValue();
    }
    if (right instanceof Double) {
      return number >= right.doubleValue();
    }
    if (right instanceof Float) {
      return number >= right.floatValue();
    }
    if (right instanceof Integer) {
      return number >= right.intValue();
    }
    if (right instanceof Long) {
      return number >= right.longValue();
    }
    if (right instanceof Short) {
      return number >= right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean lessThan(Number right) {
    if (right instanceof Byte) {
      return number < right.byteValue();
    }
    if (right instanceof Double) {
      return number < right.doubleValue();
    }
    if (right instanceof Float) {
      return number < right.floatValue();
    }
    if (right instanceof Integer) {
      return number < right.intValue();
    }
    if (right instanceof Long) {
      return number < right.longValue();
    }
    if (right instanceof Short) {
      return number < right.shortValue();
    }
    throw new UnsupportedOperationException();
  }

  @Override
  public Boolean lessThanEq(Number right) {
    if (right instanceof Byte) {
      return number <= right.byteValue();
    }
    if (right instanceof Double) {
      return number <= right.doubleValue();
    }
    if (right instanceof Float) {
      return number <= right.floatValue();
    }
    if (right instanceof Integer) {
      return number <= right.intValue();
    }
    if (right instanceof Long) {
      return number <= right.longValue();
    }
    if (right instanceof Short) {
      return number <= right.shortValue();
    }
    throw new UnsupportedOperationException();
  }
}
