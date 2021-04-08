import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;

public class MultipleBinaryExpressions {

  private final String ONE_STRING = "1";
  private final String TWO_STRING = "2";
  private final String THREE_STRING = "3";
  private final String FOUR_STRING = "4";
  private final String FIVE_STRING = "5";
  private final String SIX_STRING = "6";
  private final String SEVEN_STRING = "7";
  private final String EIGHT_STRING = "8";
  private final String NINE_STRING = "9";

  public final @StringVal("123456789123456789") String concat1 =
      ONE_STRING
          + TWO_STRING
          + THREE_STRING
          + FOUR_STRING
          + FIVE_STRING
          + SIX_STRING
          + SEVEN_STRING
          + EIGHT_STRING
          + NINE_STRING
          + ONE_STRING
          + TWO_STRING
          + THREE_STRING
          + FOUR_STRING
          + FIVE_STRING
          + SIX_STRING
          + SEVEN_STRING
          + EIGHT_STRING
          + NINE_STRING;

  public final @StringVal("112233445566778899") String concat2 =
      ONE_STRING
          + "1"
          + TWO_STRING
          + "2"
          + THREE_STRING
          + "3"
          + FOUR_STRING
          + "4"
          + FIVE_STRING
          + "5"
          + "6"
          + SIX_STRING
          + "7"
          + SEVEN_STRING
          + "8"
          + EIGHT_STRING
          + "9"
          + NINE_STRING;

  private final int ONE = 1;
  private final int TWO = 2;
  private final int THREE = 3;
  private final int FOUR = 4;
  private final int FIVE = 5;
  private final int SIX = 6;
  private final int SEVEN = 7;
  private final int EIGHT = 8;
  private final int NINE = 9;

  public final @IntVal(90) int plus1 =
      ONE + TWO + THREE + FOUR + FIVE + SIX + SEVEN + EIGHT + NINE + ONE + TWO + THREE + FOUR + FIVE
          + SIX + SEVEN + EIGHT + NINE;
  public final @IntVal(90) int plus2 =
      ONE + 1 + TWO + 2 + THREE + 3 + FOUR + 4 + FIVE + 5 + SIX + 6 + SEVEN + 7 + EIGHT + 8 + NINE
          + 9;
}
