package org.checkerframework.common.value.util;

public abstract class NumberMath<T extends Number> {
    public static NumberMath<?> getNumberMath(Number number) {
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

    public abstract Number plus(Number right);

    public abstract Number minus(Number right);

    public abstract Number times(Number right);

    /**
     * Returns the result of dividing the {@code this} by {@code right}. If {@code right} is zero
     * and this is an integer division, {@code null} is returned.
     */
    public abstract Number divide(Number right);

    /**
     * Returns the result of {@code this % right}. If {@code right} is zero and this is an integer
     * remainder, {@code null} is returned.
     */
    public abstract Number remainder(Number right);

    public abstract Number shiftLeft(Number right);

    public abstract Number signedShiftRight(Number right);

    public abstract Number unsignedShiftRight(Number right);

    public abstract Number bitwiseAnd(Number right);

    public abstract Number bitwiseOr(Number right);

    public abstract Number bitwiseXor(Number right);

    public abstract Number unaryPlus();

    public abstract Number unaryMinus();

    public abstract Number bitwiseComplement();

    public abstract Boolean equalTo(Number right);

    public abstract Boolean notEqualTo(Number right);

    public abstract Boolean greaterThan(Number right);

    public abstract Boolean greaterThanEq(Number right);

    public abstract Boolean lessThan(Number right);

    public abstract Boolean lessThanEq(Number right);

    public static boolean isIntegralZero(Number number) {
        if (number instanceof Byte) {
            return number.byteValue() == 0;
        } else if (number instanceof Integer) {
            return number.intValue() == 0;
        } else if (number instanceof Long) {
            return number.longValue() == 0;
        } else if (number instanceof Short) {
            return number.shortValue() == 0;
        }
        return false;
    }
}
