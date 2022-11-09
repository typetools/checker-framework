package org.plumelib.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.PolyUpperBound;
import org.checkerframework.checker.lock.qual.GuardSatisfied;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.common.value.qual.StaticallyExecutable;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.dataflow.qual.SideEffectFree;

/** Mathematical utilities. */
public final class MathPlume {

  /**
   * Returns z such that {@code (z == x mod y) && (0 <= z < abs(y))}. This should really be named
   * {@code modNonnegative} rather than {@code modPositive}.
   *
   * @param x value to be modded
   * @param y modulus
   * @return x % y, where the result is constrained to be non-negative
   * @deprecated use {@link #modNonnegative(long, long)}
   */
  @Deprecated // use modNonnegative(); deprecated 2020-02-20
  @Pure
  @StaticallyExecutable
  public static @NonNegative @LessThan("#2") @PolyUpperBound long modPositive(
      long x, @PolyUpperBound long y) {
    return 0;
  }

  /**
   * Returns a tuple of (r,m) where no number in NUMS is equal to r (mod m) but for every number in
   * NUMS, at least one is equal to every non-r remainder. The modulus is chosen as small as
   * possible, but no greater than half the range of the input numbers (else null is returned).
   *
   * @param nums the list of operands
   * @return a (remainder, modulus) pair that fails to match elements of nums
   */
  // This seems to give too many false positives (or maybe my probability
  // model was wrong); use nonmodulusStrict instead.
  @SuppressWarnings("allcheckers:purity")
  @Pure
  @StaticallyExecutable
  public static long @Nullable @ArrayLen(2) [] nonmodulusNonstrict(long[] nums) {
    if (nums.length < 4) {
      return null;
    }
    int maxModulus = (int) Math.min(nums.length / 2, ArraysPlume.elementRange(nums) / 2);

    // System.out.println("nums.length=" + nums.length + ", range=" +
    // ArraysPlume.elementRange(nums) + ", maxModulus=" + maxModulus);

    // no real sense checking 2, as commonModulus would have found it, but
    // include it to make this function stand on its own
    for (int m = 2; m <= maxModulus; m++) {
      // System.out.println("Trying m=" + m);
      boolean[] hasModulus = new boolean[m]; // initialized to false?
      int numNonmodulus = m;
      for (int i = 0; i < nums.length; i++) {
        @IndexFor("hasModulus") int rem = (int) modPositive(nums[i], m);
        if (!hasModulus[rem]) {
          hasModulus[rem] = true;
          numNonmodulus--;
          // System.out.println("rem=" + rem + " for " + nums[i] + "; numNonmodulus=" +
          // numNonmodulus);
          if (numNonmodulus == 0) {
            // Quit as soon as we see every remainder instead of processing
            // each element of the input list.
            break;
          }
        }
      }
      // System.out.println("For m=" + m + ", numNonmodulus=" + numNonmodulus);
      if (numNonmodulus == 1) {
        return new long[] {ArraysPlume.indexOf(hasModulus, false), m};
      }
    }
    return null;
  }
}
