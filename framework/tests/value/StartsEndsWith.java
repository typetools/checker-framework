// Tests string length refinement after startsWith or endsWith return true
// https://github.com/kelloggm/checker-framework/issues/56

import org.checkerframework.common.value.qual.ArrayLen;
import org.checkerframework.common.value.qual.MinLen;

public class StartsEndsWith {

    void refineStartsWith(String str) {
        if (str.startsWith("prefix")) {
            @MinLen(6) String s6 = str;
            // :: error: (assignment.type.incompatible)
            @MinLen(7) String s7 = str;
        } else {
            // :: error: (assignment.type.incompatible)
            @MinLen(6) String s6 = str;
        }
    }

    void refineEndsWith(String str) {
        if (str.endsWith("suffix")) {
            @MinLen(6) String s6 = str;
            // :: error: (assignment.type.incompatible)
            @MinLen(7) String s7 = str;
        } else {
            // :: error: (assignment.type.incompatible)
            @MinLen(6) String s6 = str;
        }
    }

    void refineStartsEndsWith(String str) {
        if (str.startsWith("longprefix") && str.endsWith("prefix")) {
            @MinLen(10) String s10 = str;
            // :: error: (assignment.type.incompatible)
            @MinLen(11) String s11 = str;
        }
    }

    void refineStartsArrayLen(String str, @ArrayLen(10) String prefix) {
        if (str.startsWith(prefix)) {
            @MinLen(10) String sg10 = str;
            // :: error: (assignment.type.incompatible)
            @ArrayLen(10) String s10 = str;
        }
    }

    void noRefinement(@ArrayLen(10) String str) {
        if (str.startsWith("x")) {
            @ArrayLen(10) String s10 = str;
        }
    }

    void refineStartsStaticFinal(String str) {
        if (str.startsWith(StartsEndsWithExternal.staticFinalField)) {
            @MinLen(3) String s3 = str;
        }
    }
}

class StartsEndsWithExternal {
    public static final String staticFinalField = "str";
}
