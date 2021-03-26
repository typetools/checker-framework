import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.StringVal;

public class StringValNullConcatLength {
  @StringVal("a") String string1;

  @StringVal("b") String string2;

  @ArrayLen({1, 2}) String string3;

  @ArrayLen({2, 3}) String string4;

  @ArrayLenRange(from = 1, to = 3) String string5;

  @StringVal({"anull", "ab", "nullb", "nullnull"}) String string6 = string1 + string2;

  @ArrayLen({2, 3, 5, 6, 8}) String string7 = string1 + string3;

  @ArrayLen({3, 4, 5, 6, 7, 8}) String string8 = string3 + string4;

  @ArrayLenRange(from = 2, to = 8) String string10 = string1 + string5;

  // Omitting that string2 can be null
  // :: error: (assignment.type.incompatible)
  @ArrayLen({3, 4}) String string9 = string2 + string4;
}
