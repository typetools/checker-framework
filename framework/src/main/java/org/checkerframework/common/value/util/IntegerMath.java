package org.checkerframework.common.value.util;

/** Performs Java numeric operations whose left operand is an int. */
public class IntegerMath extends NumberMath<Integer> {

  /**
   * Creates an IntegerMath.
   *
   * @param number the left operand
   */
  public IntegerMath(int number) {
    super(number);
  }
}
