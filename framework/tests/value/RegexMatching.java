// A test for the @MatchesRegex annotation.

import org.checkerframework.common.value.qual.*;

class RegexMatching {
    void stringConstants() {
        @MatchesRegex("a*")
        String a = "a";
        @MatchesRegex("a*")
        String blank = "";
        // :: error: assignment.type.incompatible
        @MatchesRegex("a*")
        String b = "b";

        @MatchesRegex("a")
        String a1 = "a";
        // :: error: assignment.type.incompatible
        @MatchesRegex("a")
        String blank1 = "";
        // :: error: assignment.type.incompatible
        @MatchesRegex("a")
        String b1 = "b";

        @MatchesRegex("\\s")
        String space = " ";
        @MatchesRegex("\\s")
        String severalSpaces = "      ";
        // :: error: assignment.type.incompatible
        @MatchesRegex("\\s")
        String b2 = "b";

        @MatchesRegex("[^abc]")
        String d = "d";
        @MatchesRegex("[^abc]")
        String d1 = String.valueOf(new char[] {'d'});
        // :: error: assignment.type.incompatible
        @MatchesRegex("[^abc]")
        String c = "c";
    }

    void severalString(@StringVal({"a", "aa"}) String aaa, @StringVal({"aa", "b"}) String aab) {
        @MatchesRegex("a*")
        String a = aaa;
        // :: error: assignment.type.incompatible
        @MatchesRegex("a*")
        String a1 = aab;

        @MatchesRegex("a+")
        String a2 = aaa;
        // :: error: assignment.type.incompatible
        @MatchesRegex("a+")
        String a3 = aab;
    }
}
