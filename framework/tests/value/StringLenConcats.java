import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.StringVal;

class StringLenConcats {

    void stringLenConcat(@ArrayLen(3) String a, @ArrayLen(5) String b) {
        @ArrayLen(8) String ab = a + b;
        @ArrayLen(7) String bxx = b + "xx";
    }

    void stringLenRangeConcat(
            @ArrayLenRange(from = 3, to = 5) String a,
            @ArrayLenRange(from = 11, to = 19) String b) {
        @ArrayLenRange(from = 14, to = 24) String ab = a + b;
        @ArrayLenRange(from = 13, to = 21) String bxx = b + "xx";
    }

    void stringValLenConcat(
            @StringVal("constant") String a,
            @StringVal({"a", "b", "c"}) String b,
            @StringVal({"a", "xxx"}) String c,
            @ArrayLen(11) String d) {

        @ArrayLen(19) String ad = a + d;
        @ArrayLen(12) String bd = b + d;
        @ArrayLenRange(from = 12, to = 14) String cd = c + d;
    }

    void stringValLenRangeConcat(
            @StringVal("constant") String a,
            @StringVal({"a", "b", "c"}) String b,
            @StringVal({"a", "xxx"}) String c,
            @ArrayLenRange(from = 11, to = 19) String d) {

        @ArrayLenRange(from = 19, to = 27) String ad = a + d;
        @ArrayLenRange(from = 12, to = 20) String bd = b + d;
        @ArrayLenRange(from = 12, to = 22) String cd = c + d;
    }

    void tooManyStringValConcat(
            @StringVal({"a", "b", "c", "d"}) String a,
            @StringVal({"ee", "ff", "gg", "hh", "ii"}) String b) {
        @ArrayLen(2) String aa = a + a;
        @ArrayLen(3) String ab = a + b;
    }

    void conversions(
            @IntVal(123) int intConst,
            @IntRange(from = -100000, to = 100) int intRange,
            @IntRange(from = 100, to = 100000) int positiveRange,
            @ArrayLen(10) String a,
            @ArrayLenRange(from = 10, to = 20) String b,
            @StringVal({"aaa", "bbbbb"}) String c) {
        @ArrayLen(13) String aConst = a + intConst;
        @ArrayLen({11, 12, 13, 14, 15, 16, 17}) String aRange = a + intRange;
        @ArrayLen({13, 14, 15, 16}) String aPositive = a + positiveRange;

        @ArrayLenRange(from = 13, to = 23) String bConst = b + intConst;
        @ArrayLenRange(from = 11, to = 27) String bRange = b + intRange;
        @ArrayLenRange(from = 13, to = 26) String bPositive = b + positiveRange;

        @StringVal({"aaa123", "bbbbb123"}) String cConst = c + intConst;
        @ArrayLen({4, 5, 6, 7, 8, 9, 10, 11, 12}) String cRange = c + intRange;
        @ArrayLen({6, 7, 8, 9, 10, 11}) String cPositive = c + positiveRange;
    }
}
