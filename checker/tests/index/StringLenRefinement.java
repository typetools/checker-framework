import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.ArrayLenRange;
import org.checkerframework.common.value.qual.StringVal;

public class StringLenRefinement {

    void refineLenRange(
            @ArrayLenRange(from = 3, to = 10) String range,
            @ArrayLen({4, 6, 12}) String lens,
            @StringVal({"aaaa", "bbbb", "cccccc", "dddddddddddd"}) String vals) {
        if (range.length() <= 7) {
            @ArrayLenRange(from = 3, to = 7) String shortRange = range;
        } else {
            @ArrayLenRange(from = 8, to = 10) String longRange = range;
        }

        if (lens.length() <= 7) {
            @ArrayLen({4, 6}) String shortLens = lens;
        } else {
            @ArrayLen({12}) String longLens = lens;
        }

        if (vals.length() <= 7) {
            @StringVal({"aaaa", "bbbb", "cccccc"}) String shortVals = vals;
        } else {

            @StringVal({"dddddddddddd"}) String longVals = vals;
        }
    }

    void refineLen(
            @ArrayLenRange(from = 3, to = 10) String range, @ArrayLen({4, 8, 12}) String lens) {

        if (range.length() == 5 || range.length() == 8 || range.length() == 13) {
            @ArrayLen({5, 8}) String refinedArg = range;
        }

        if (lens.length() == 5 || lens.length() == 8 || lens.length() == 13) {
            @ArrayLen({8}) String refinedLens = lens;
        }
    }
}
