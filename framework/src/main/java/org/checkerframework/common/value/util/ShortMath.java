package org.checkerframework.common.value.util;

/** Performs Java numeric operations whose left operand is a short. */
public class ShortMath extends NumberMath<Short> {

  /**
   * Creates a ShortMath.
   *
   * @param number the left operand
   */
  public ShortMath(short number) {
    super(number);
  }
}
