package org.checkerframework.framework.test.junit;

import org.checkerframework.common.value.util.NumberMath;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link NumberMath}. */
public class NumberMathTest {

  /**
   * Returns a NumberMath whose left operand is {@code number}.
   *
   * @param number the left operand
   * @return a NumberMath whose left operand is {@code number}
   */
  private static NumberMath math(Number number) {
    return NumberMath.getNumberMath(number);
  }

  /** Tests binary numeric promotion for arithmetic operations. */
  @Test
  public void arithmeticPromotion() {
    Assert.assertEquals(Integer.valueOf(5), math((byte) 2).plus((byte) 3));
    Assert.assertEquals(Long.valueOf(5), math(2).plus(3L));
    Assert.assertEquals(Float.valueOf(5), math(2L).plus(3.0f));
    Assert.assertEquals(Double.valueOf(5), math(2.0f).plus(3.0));
    Assert.assertEquals(Integer.valueOf(-1), math((short) 2).minus((byte) 3));
    Assert.assertEquals(Long.valueOf(6), math(2).times(3L));
    Assert.assertEquals(Float.valueOf(2.5f), math(5L).divide(2.0f));
    Assert.assertEquals(Double.valueOf(1), math(5.0f).remainder(2.0));
  }

  /** Tests division and remainder by zero. */
  @Test
  public void zeroDivisor() {
    Assert.assertNull(math(1).divide(0L));
    Assert.assertNull(math(1L).remainder((byte) 0));
    Assert.assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), math(1).divide(0.0));
    Assert.assertEquals(Float.valueOf(Float.NaN), math(0.0f).remainder(0));
  }

  /** Tests integral operations and their result types. */
  @Test
  public void integralOperations() {
    Assert.assertEquals(Integer.valueOf(8), math((byte) 1).shiftLeft(3L));
    Assert.assertEquals(Long.valueOf(8), math(1L).shiftLeft((byte) 3));
    Assert.assertEquals(Integer.valueOf(-2), math((short) -4).signedShiftRight(1));
    Assert.assertEquals(Long.valueOf(Long.MAX_VALUE), math(-1L).unsignedShiftRight(1));
    Assert.assertEquals(Integer.valueOf(1), math((byte) 5).bitwiseAnd((short) 3));
    Assert.assertEquals(Long.valueOf(7), math(5).bitwiseOr(2L));
    Assert.assertEquals(Integer.valueOf(6), math((short) 5).bitwiseXor((byte) 3));
    Assert.assertEquals(Integer.valueOf(-6), math((byte) 5).bitwiseComplement());
  }

  /** Tests unary numeric promotion and comparisons. */
  @Test
  public void unaryAndComparisonOperations() {
    Assert.assertEquals(Integer.valueOf(2), math((byte) 2).unaryPlus());
    Assert.assertEquals(Long.valueOf(-2), math(2L).unaryMinus());
    Assert.assertEquals(Boolean.TRUE, math(2).lessThan(3L));
    Assert.assertEquals(Boolean.TRUE, math(3L).greaterThanEq(3.0f));
    Assert.assertEquals(Boolean.FALSE, math(Double.NaN).equalTo(Double.NaN));
    Assert.assertEquals(Boolean.TRUE, math(Double.NaN).notEqualTo(Double.NaN));
  }
}
