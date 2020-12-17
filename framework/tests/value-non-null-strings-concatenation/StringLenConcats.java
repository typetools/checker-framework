import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.common.value.qual.StringVal;

public class StringLenConcats {

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

    void stringLenLenRangeConcat(
            @ArrayLen({3, 4, 5}) String a, @ArrayLenRange(from = 10, to = 100) String b) {
        @ArrayLenRange(from = 13, to = 105) String ab = a + b;
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

    void charConversions(
            char c,
            @IntVal({1, 100, 10000}) char d,
            @ArrayLen({100, 200}) String s,
            @ArrayLenRange(from = 100, to = 200) String t,
            @StringVal({"a", "bb", "ccc", "dddd"}) String u) {
        @ArrayLen({101, 201}) String sc = s + c;
        @ArrayLen({101, 201}) String sd = s + d;

        @ArrayLenRange(from = 101, to = 201) String tc = t + c;

        @ArrayLen({2, 3, 4, 5}) String uc = u + c;
        @ArrayLen({2, 3, 4, 5}) String ud = u + d;
    }

    void intConversions(
            @IntVal(123) int intConst,
            @IntRange(from = -100000, to = 100) int intRange,
            @IntRange(from = 100, to = 100000) int positiveRange,
            int unknownInt,
            @ArrayLen(10) String a,
            @ArrayLenRange(from = 10, to = 20) String b,
            @StringVal({"aaa", "bbbbb"}) String c) {
        @ArrayLen(13) String aConst = a + intConst;
        @ArrayLen({11, 12, 13, 14, 15, 16, 17}) String aRange = a + intRange;
        @ArrayLen({13, 14, 15, 16}) String aPositive = a + positiveRange;
        @ArrayLenRange(from = 11, to = 21) String aUnknown = a + unknownInt;

        @ArrayLenRange(from = 13, to = 23) String bConst = b + intConst;
        @ArrayLenRange(from = 11, to = 27) String bRange = b + intRange;
        @ArrayLenRange(from = 13, to = 26) String bPositive = b + positiveRange;
        @ArrayLenRange(from = 11, to = 31) String bUnknown = b + unknownInt;

        @StringVal({"aaa123", "bbbbb123"}) String cConst = c + intConst;
        @ArrayLen({4, 5, 6, 7, 8, 9, 10, 11, 12}) String cRange = c + intRange;
        @ArrayLen({6, 7, 8, 9, 10, 11}) String cPositive = c + positiveRange;
    }

    void longConversions(
            @IntVal(1000000000000l) long longConst,
            @IntRange(from = 10, to = 1000000000000l) long longRange,
            long unknownLong,
            @ArrayLen(10) String a) {

        @ArrayLen(23) String aConst = a + longConst;
        @ArrayLenRange(from = 12, to = 23) String aRange = a + longRange;
        @ArrayLenRange(from = 11, to = 30) String aUnknown = a + unknownLong;
    }

    void byteConversions(
            @IntVal(100) byte byteConst,
            @IntRange(from = 2, to = 10) byte byteRange,
            byte unknownByte,
            @ArrayLen(10) String a) {

        @ArrayLen(13) String aConst = a + byteConst;
        @ArrayLenRange(from = 11, to = 12) String aRange = a + byteRange;
        @ArrayLenRange(from = 11, to = 14) String aUnknown = a + unknownByte;
    }

    void minLenConcat(@MinLen(5) String s, @MinLen(7) String t) {
        @MinLen(12) String st = s + t;
    }
}
