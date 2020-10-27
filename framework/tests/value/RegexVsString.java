// A test for the @MatchesRegex annotation.

import org.checkerframework.common.value.qual.*;

class RegexVsString {
    void stringToRegex1(@StringVal({"(a)"}) String a) {
        // :: error: assignment.type.incompatible
        @MatchesRegex("(a)") String a2 = a;
    }

    void stringToRegex2(@StringVal({"a"}) String a) {
        @MatchesRegex("(a)") String a2 = a;
    }

    void stringToRegex3(@StringVal({"a"}) String a) {
        @MatchesRegex("^a$") String a2 = a;
    }

    void regexToString(@MatchesRegex("^a$") String a) {
        // TODO: This is a false positive.  In the future, eliminate it.
        // :: error: assignment.type.incompatible
        @StringVal({"a"}) String a2 = a;
    }
}
