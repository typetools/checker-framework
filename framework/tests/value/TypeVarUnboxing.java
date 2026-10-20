import org.checkerframework.common.value.qual.BoolVal;
import org.checkerframework.common.value.qual.DoubleVal;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;

/** Regression test for evaluating unboxing of a type variable with a known value. */
public class TypeVarUnboxing {
  <T extends Byte> void increment(@IntVal(1) T t) {
    // :: error: [unary.increment]
    t++;
  }

  <T extends Byte> void byteValues(@IntVal(1) T input) {
    @IntVal({1, 2, 3}) T t = input;
    @IntVal(1) byte before = t++;
    @IntVal(2) byte after = t;
    @IntVal(2) byte beforeDecrement = t--;
    @IntVal(1) byte afterDecrement = t;
    // :: error: [assignment]
    @IntVal(2) byte wrong = t;
  }

  <T extends Short> void shortValue(@IntVal(1) T t) {
    @IntVal(2) int result = t + 1;
  }

  <T extends Character> void characterValue(@IntVal(65) T t) {
    @IntVal(65) char result = t;
    // :: error: [assignment]
    @IntVal(66) char wrong = t;
  }

  <T extends Integer> void integerValue(@IntVal(1) T t) {
    @IntVal(2) int result = t + 1;
  }

  <T extends Long> void longValue(@IntVal(1) T t) {
    @IntVal(2) long result = t + 1;
  }

  <T extends Float> void floatValue(@DoubleVal(1.5) T t) {
    @DoubleVal(2.5) float result = t + 1;
  }

  <T extends Double> void doubleValue(@DoubleVal(1.5) T t) {
    @DoubleVal(2.5) double result = t + 1;
  }

  <T extends Boolean> void booleanValue(@BoolVal(true) T t) {
    @BoolVal(true) boolean result = t;
    @BoolVal(false) boolean negated = !t;
  }

  <T extends Byte, U extends T> void chainedBounds(@IntVal(1) U t) {
    @IntVal(1) byte result = t;
  }

  <T extends Byte & java.io.Serializable> void intersectionBound(@IntVal(1) T t) {
    @IntVal(1) byte result = t;
  }

  <T extends Integer> void intRange(@IntRange(from = 0, to = 10) T t) {
    @IntRange(from = 0, to = 10) int result = t;
    // :: error: [assignment]
    @IntRange(from = 0, to = 5) int wrong = t;
  }
}
