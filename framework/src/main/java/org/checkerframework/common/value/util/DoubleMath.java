package org.checkerframework.common.value.util;

/** Performs Java numeric operations whose left operand is a double. */
public class DoubleMath extends NumberMath<Double> {

  /**
   * Creates a DoubleMath.
   *
   * @param number the left operand
   */
  public DoubleMath(double number) {
    super(number);
  }
}
