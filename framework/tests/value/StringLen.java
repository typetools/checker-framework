import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.IntRange;
import org.checkerframework.common.value.qual.IntVal;
import org.checkerframework.common.value.qual.MinLen;
import org.checkerframework.common.value.qual.StringVal;

class StringLen {
    void stringValArrayLen(
            @StringVal("") String empty,
            @StringVal("const") String constant,
            @StringVal({"s", "longconstant"}) String values,
            String unknown) {

        // Compatibility with ArrayLen
        @ArrayLen(0) String len0 = empty;
        @ArrayLen(5) String len5 = constant;
        @ArrayLen({1, 12}) String len1_12 = values;

        // Compatibility with ArrayLenRange
        @ArrayLenRange(from = 0, to = 0) String rng0 = empty;
        @ArrayLenRange(from = 5, to = 5) String rng5 = constant;
        @ArrayLenRange(from = 1, to = 12) String rng1_12 = values;

        // :: error: (assignment.type.incompatible)
        @ArrayLen(4) String len4 = constant;
        // :: error: (assignment.type.incompatible)
        @ArrayLenRange(from = 1, to = 11) String rng1_10 = values;
    }

    void stringValLubToArrayLen(
            boolean flag,
            @StringVal({"a", "b", "c", "d", "e"}) String ae,
            @StringVal({"f", "g", "h", "i", "j", "k"}) String fk,
            @StringVal({"ffff", "gggg", "hhhh", "iiii", "jjjj", "kkkkkkk"}) String fkR) {

        @ArrayLen(1) String ak = flag ? ae : fk;
        @ArrayLen({1, 4, 7}) String akR = flag ? ae : fkR;
    }

    void stringValLubToArrayLenRange(
            boolean flag,
            @StringVal({"a", "bb", "ccc", "dddd", "eeeee"}) String ae,
                    @StringVal({"ffffff", "ggggggg", "hhhhhhhh", "iiiiiiiii", "jjjjjjjjjj", "kkkkkkkkkkk"}) String fk) {

        @ArrayLenRange(from = 1, to = 11) String ak = flag ? ae : fk;
    }

    void arrayLenStringVal(
            @ArrayLen(0) String len0,
            @ArrayLenRange(from = 0, to = 0) String rng0,
            @ArrayLen({0, 1}) String nonEmpty) {
        @StringVal("") String emptyLen = len0;
        @StringVal("") String emptyRng = rng0;

        // :: error: (assignment.type.incompatible)
        @StringVal("") String emptyError = nonEmpty;
        // :: error: (assignment.type.incompatible)
        @StringVal("a") String nonEmptyError = nonEmpty;
    }

    void stringValLength(
            @StringVal("") String empty,
            @StringVal("const") String constant,
            @StringVal({"s", "longconstant"}) String values,
            String unknown) {

        @IntVal(0) int len0 = empty.length();
        @IntVal(5) int len5 = constant.length();
        @IntVal({1, 12}) int len1_12 = values.length();

        // :: error: (assignment.type.incompatible)
        @IntVal({1, 11}) int len1_11 = values.length();
    }

    void arrayLenLength(
            @ArrayLen(0) String empty,
            @ArrayLen(5) String constant,
            @ArrayLen({1, 12}) String values,
            String unknown) {

        @IntVal(0) int len0 = empty.length();
        @IntVal(5) int len5 = constant.length();
        @IntVal({1, 12}) int len1_12 = values.length();

        // :: error: (assignment.type.incompatible)
        @IntVal({1, 11}) int len1_11 = values.length();
    }

    void arrayLenRangeLength(
            @ArrayLenRange(from = 0, to = 0) String empty,
            @ArrayLenRange(from = 5, to = 5) String constant,
            @ArrayLenRange(from = 1, to = 12) String values,
            String unknown) {

        @IntRange(from = 0, to = 0) int len0 = empty.length();
        @IntRange(from = 5, to = 5) int len5 = constant.length();
        @IntRange(from = 1, to = 12) int len1_12 = values.length();

        // :: error: (assignment.type.incompatible)
        @IntRange(from = 1, to = 11) int len1_11 = values.length();
    }

    void minLenLength(@MinLen(5) String s) {
        @IntRange(from = 5) int l = s.length();
    }

    void arrayCast(@ArrayLen(1) String array) {
        @ArrayLen(1) String cast1 = (String) array;
        @ArrayLen(1) String cast2 = (@ArrayLen(1) String) array;
    }
}
