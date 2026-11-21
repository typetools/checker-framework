// Test case for expressions whose type is a type variable bounded by a primitive wrapper, used as
// operands of specially-handled methods and operators.

import java.util.Random;
import org.checkerframework.checker.index.qual.IndexFor;
import org.checkerframework.checker.index.qual.LTLengthOf;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.index.qual.Positive;

public class TypeVarArguments {
  <T extends @IndexFor("#2") Integer> int min(T t, int[] a, @NonNegative int x) {
    return a[Math.min(t, x)];
  }

  <T extends @NonNegative Integer> @NonNegative int max(T t, int x) {
    return Math.max(t, x);
  }

  <T extends @Positive Integer> int nextInt(Random rand, T t) {
    return rand.nextInt(t);
  }

  <T extends Integer> int nextIntUnknown(Random rand, T t) {
    // :: error: [argument]
    return rand.nextInt(t);
  }

  <T extends @NonNegative Integer> boolean notEquals(T t, @NonNegative Integer x) {
    return t != x;
  }

  <T extends @LTLengthOf("#2") Integer> boolean lessThan(T t, int[] a) {
    return t < a.length;
  }

  <T extends @IndexFor("#2") Integer> int shift(T t, int[] a) {
    return a[t >> 1];
  }

  <T extends @IndexFor("#2") Integer> int and(T t, int[] a, int mask) {
    return a[t & mask];
  }

  <T extends @IndexFor("#2") Integer> int remainder(T t, int[] a, int x) {
    return a[t % x];
  }
}
