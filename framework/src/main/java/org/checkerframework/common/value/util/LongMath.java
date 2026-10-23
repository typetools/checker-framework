package org.checkerframework.common.value.util;

/** Performs Java numeric operations whose left operand is a long. */
public class LongMath extends NumberMath<Long> {

  /**
   * Creates a LongMath.
   *
   * @param number the left operand
   */
  public LongMath(long number) {
    super(number);
  }
}
