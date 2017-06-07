import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
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
}
