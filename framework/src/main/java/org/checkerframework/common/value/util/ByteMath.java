package org.checkerframework.common.value.util;

/** Performs Java numeric operations whose left operand is a byte. */
public class ByteMath extends NumberMath<Byte> {

  /**
   * Creates a ByteMath.
   *
   * @param number the left operand
   */
  public ByteMath(byte number) {
    super(number);
  }
}
