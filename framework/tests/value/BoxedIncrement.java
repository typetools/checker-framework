import org.checkerframework.common.value.qual.IntVal;

// Test case for evaluating the synthetic boxing call in an increment or decrement of a boxed
// variable with a known value.
public class BoxedIncrement {
  void byteIncrement(@IntVal(1) Byte input) {
    Byte b = input;
    b++;
    @IntVal(2) byte after = b;
    b--;
    @IntVal(1) byte afterDecrement = b;
    // :: error: [assignment]
    @IntVal(2) byte wrong = b;
  }

  void shortIncrement(@IntVal(1) Short input) {
    Short s = input;
    ++s;
    @IntVal(2) short after = s;
  }

  void characterIncrement(@IntVal(65) Character input) {
    Character c = input;
    c++;
    @IntVal(66) char after = c;
  }
}
