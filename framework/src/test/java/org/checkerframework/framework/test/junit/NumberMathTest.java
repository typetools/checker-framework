package org.checkerframework.framework.test.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.checkerframework.common.value.util.ByteMath;
import org.checkerframework.common.value.util.DoubleMath;
import org.checkerframework.common.value.util.FloatMath;
import org.checkerframework.common.value.util.IntegerMath;
import org.checkerframework.common.value.util.LongMath;
import org.checkerframework.common.value.util.NumberMath;
import org.checkerframework.common.value.util.ShortMath;
import org.junit.Test;

/** Tests for {@link NumberMath}. */
public class NumberMathTest {

  /** Tests binary numeric promotion for arithmetic operations. */
  @Test
  public void arithmeticPromotion() {
    assertEquals(Integer.valueOf(5), new ByteMath((byte) 2).plus((byte) 3));
    assertEquals(Long.valueOf(5), new IntegerMath(2).plus(3L));
    assertEquals(Float.valueOf(5), new LongMath(2).plus(3.0f));
    assertEquals(Double.valueOf(5), new FloatMath(2).plus(3.0));
    assertEquals(Integer.valueOf(-1), new ShortMath((short) 2).minus((byte) 3));
    assertEquals(Long.valueOf(6), new IntegerMath(2).times(3L));
    assertEquals(Float.valueOf(2.5f), new LongMath(5).divide(2.0f));
    assertEquals(Double.valueOf(1), new FloatMath(5).remainder(2.0));
  }

  /** Tests division and remainder by zero. */
  @Test
  public void zeroDivisor() {
    assertNull(new IntegerMath(1).divide(0L));
    assertNull(new LongMath(1).remainder((byte) 0));
    assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), new IntegerMath(1).divide(0.0));
    assertEquals(Float.valueOf(Float.NaN), new FloatMath(0).remainder(0));
  }

  /** Tests integral operations and their result types. */
  @Test
  public void integralOperations() {
    assertEquals(Integer.valueOf(8), new ByteMath((byte) 1).shiftLeft(3L));
    assertEquals(Long.valueOf(8), new LongMath(1).shiftLeft((byte) 3));
    assertEquals(Integer.valueOf(-2), new ShortMath((short) -4).signedShiftRight(1));
    assertEquals(Long.valueOf(Long.MAX_VALUE), new LongMath(-1).unsignedShiftRight(1));
    assertEquals(Integer.valueOf(1), new ByteMath((byte) 5).bitwiseAnd((short) 3));
    assertEquals(Long.valueOf(7), new IntegerMath(5).bitwiseOr(2L));
    assertEquals(Integer.valueOf(6), new ShortMath((short) 5).bitwiseXor((byte) 3));
    assertEquals(Integer.valueOf(-6), new ByteMath((byte) 5).bitwiseComplement());
  }

  /** Tests unary numeric promotion and comparisons. */
  @Test
  public void unaryAndComparisonOperations() {
    assertEquals(Integer.valueOf(2), new ByteMath((byte) 2).unaryPlus());
    assertEquals(Long.valueOf(-2), new LongMath(2).unaryMinus());
    assertEquals(Boolean.TRUE, new IntegerMath(2).lessThan(3L));
    assertEquals(Boolean.TRUE, new LongMath(3).greaterThanEq(3.0f));
    assertEquals(Boolean.FALSE, new DoubleMath(Double.NaN).equalTo(Double.NaN));
    assertEquals(Boolean.TRUE, new DoubleMath(Double.NaN).notEqualTo(Double.NaN));
  }
}
