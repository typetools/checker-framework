// A test for the @DoesNotMatchRegex annotation.

import org.checkerframework.common.value.qual.*;

public class RegexNonMatching {

    void stringConstants() {
        // :: error: assignment
        @DoesNotMatchRegex("a*") String aStar = "a";
        // :: error: assignment
        aStar = "";
        aStar = "b";
        @DoesNotMatchRegex({"a+"}) String aPlus = "b";
        // @DoesNotMatchRegex("a+") String aPlus = "b";
        aStar = "ab";
        aStar = "baa";

        // :: error: assignment
        @DoesNotMatchRegex("a") String a1 = "a";
        @DoesNotMatchRegex("a") String blank1 = "";
        @DoesNotMatchRegex("a") String b1 = "b";

        // :: error: assignment
        @DoesNotMatchRegex("\\s") String space = " ";
        // :: error: assignment
        @DoesNotMatchRegex("\\s+") String severalSpaces = "      ";
        // TODO: this should work
        @DoesNotMatchRegex("\\s") String b2 = "b";

        // :: error: assignment
        @DoesNotMatchRegex("[^abc]") String d = "d";
        // :: error: assignment
        @DoesNotMatchRegex("[^abc]") String d1 = String.valueOf(new char[] {'d'});
        // TODO: this should work, shouldn't it?
        @DoesNotMatchRegex("[^abc]") String c = "c";
    }

    void severalString(@StringVal({"a", "aa"}) String aaa, @StringVal({"aa", "b"}) String aab) {
        // :: error: assignment
        @DoesNotMatchRegex("a*") String a = aaa;
        // :: error: assignment
        @DoesNotMatchRegex("a*") String a1 = aab;

        // :: error: assignment
        @DoesNotMatchRegex("a+") String a2 = aaa;
        // :: error: assignment
        @DoesNotMatchRegex("a+") String a3 = aab;
    }

    void multipleRegexes(@StringVal({"a", "aa"}) String aaa, @StringVal({"aa", "b"}) String aab) {
        // :: error: assignment
        @DoesNotMatchRegex({"a*", "b*"}) String a = aaa;
        // :: error: assignment
        @DoesNotMatchRegex({"a*", "b*"}) String a1 = aab;

        // :: error: assignment
        @DoesNotMatchRegex({"aa", "b*"}) String a2 = aaa;
        // :: error: assignment
        @DoesNotMatchRegex({"aa", "b*"}) String a3 = aab;
    }

    void regexSubtypingConstant(@DoesNotMatchRegex({"a", "b"}) String ab) {
        @DoesNotMatchRegex("a") String a = ab;
        // :: error: assignment
        @DoesNotMatchRegex("c") String c = ab;
        @DoesNotMatchRegex({"a", "b"}) String ab1 = ab;
        // :: error: assignment
        @DoesNotMatchRegex({"a", "b", "c"}) String abc = ab;
        // :: error: assignment
        @StringVal("a") String a1 = ab;
        // :: error: assignment
        @StringVal({"a", "b"}) String ab2 = ab;
        // :: error: assignment
        @StringVal({"a", "b", "c"}) String abc1 = ab;
    }

    void regexSubtyping2(@DoesNotMatchRegex({"a*", "b*"}) String ab) {
        @DoesNotMatchRegex("a*") String a = ab;
        @DoesNotMatchRegex({"a*", "b*"}) String ab1 = ab;
        // :: error: assignment
        @DoesNotMatchRegex({"a*", "b*", "c*"}) String abc = ab;
        // :: error: assignment
        @StringVal("a*") String a1 = ab;
        // :: error: assignment
        @StringVal({"a*", "b*"}) String ab2 = ab;
        // :: error: assignment
        @StringVal({"a*", "b*", "c*"}) String abc1 = ab;
        // :: error: assignment
        @StringVal({"c*"}) String c = ab;
    }

    void lubRegexes(
            @DoesNotMatchRegex({"a*"}) String astar,
            @DoesNotMatchRegex({"b*"}) String bstar,
            boolean b) {
        String s;
        if (b) {
            s = astar;
        } else {
            s = bstar;
        }
        // :: error: assignment
        @DoesNotMatchRegex({"a*", "b*"}) String s1 = s;
        // :: error: assignment
        @DoesNotMatchRegex({"a*"}) String s2 = s;
        // :: error: assignment
        @DoesNotMatchRegex({"b*"}) String s3 = s;
        s3 = s1;
        // :: error: assignment
        @DoesNotMatchRegex({}) String s4 = s;
        // :: error: assignment
        @DoesNotMatchRegex({".*"}) String s5 = s;
        @UnknownVal() String s6 = s;
    }

    void lubRegexWithStringVal(
            @DoesNotMatchRegex({"a*"}) String astar,
            @StringVal({"a", "aa", "b", "bb", "c", "cc"}) String dnmsNone,
            @StringVal({"aa", "b", "bb", "c", "cc"}) String dnmsA,
            @StringVal({"a", "aa", "bb", "c", "cc"}) String dnmsB,
            @StringVal({"a", "aa", "b", "bb", "cc"}) String dnmsC,
            @StringVal({"aa", "bb", "c", "cc"}) String dnmsAB,
            @StringVal({"aa", "b", "bb", "cc"}) String dnmsAC,
            @StringVal({"a", "aa", "bb", "cc"}) String dnmsBC,
            @StringVal({"aa", "bb", "cc"}) String dnmsABC) {

        @DoesNotMatchRegex({}) String dnmNone;
        @DoesNotMatchRegex({"a"}) String dnmA;
        @DoesNotMatchRegex({"b"}) String dnmB;
        @DoesNotMatchRegex({"c"}) String dnmC;
        @DoesNotMatchRegex({"a", "b"}) String dnmAB;
        @DoesNotMatchRegex({"a", "c"}) String dnmAC;
        @DoesNotMatchRegex({"b", "c"}) String dnmBC;
        @DoesNotMatchRegex({"a", "b", "c"}) String dnmABC;

        dnmNone = dnmsNone;
        // :: error: assignment
        dnmA = dnmsNone;
        // :: error: assignment
        dnmB = dnmsNone;
        // :: error: assignment
        dnmC = dnmsNone;
        // :: error: assignment
        dnmAB = dnmsNone;
        // :: error: assignment
        dnmAC = dnmsNone;
        // :: error: assignment
        dnmBC = dnmsNone;
        // :: error: assignment
        dnmABC = dnmsNone;

        dnmNone = dnmsA;
        dnmA = dnmsA;
        // :: error: assignment
        dnmB = dnmsA;
        // :: error: assignment
        dnmC = dnmsA;
        // :: error: assignment
        dnmAB = dnmsA;
        // :: error: assignment
        dnmAC = dnmsA;
        // :: error: assignment
        dnmBC = dnmsA;
        // :: error: assignment
        dnmABC = dnmsA;

        dnmNone = dnmsB;
        // :: error: assignment
        dnmA = dnmsB;
        dnmB = dnmsB;
        // :: error: assignment
        dnmC = dnmsB;
        // :: error: assignment
        dnmAB = dnmsB;
        // :: error: assignment
        dnmAC = dnmsB;
        // :: error: assignment
        dnmBC = dnmsB;
        // :: error: assignment
        dnmABC = dnmsB;

        dnmNone = dnmsC;
        // :: error: assignment
        dnmA = dnmsC;
        // :: error: assignment
        dnmB = dnmsC;
        dnmC = dnmsC;
        // :: error: assignment
        dnmAB = dnmsC;
        // :: error: assignment
        dnmAC = dnmsC;
        // :: error: assignment
        dnmBC = dnmsC;
        // :: error: assignment
        dnmABC = dnmsC;

        dnmNone = dnmsAC;
        dnmA = dnmsAC;
        // :: error: assignment
        dnmB = dnmsAC;
        dnmC = dnmsAC;
        // :: error: assignment
        dnmAB = dnmsAC;
        dnmAC = dnmsAC;
        // :: error: assignment
        dnmBC = dnmsAC;
        // :: error: assignment
        dnmABC = dnmsAC;

        dnmNone = dnmsAB;
        dnmA = dnmsAB;
        dnmB = dnmsAB;
        // :: error: assignment
        dnmC = dnmsAB;
        dnmAB = dnmsAB;
        // :: error: assignment
        dnmAC = dnmsAB;
        // :: error: assignment
        dnmBC = dnmsAB;
        // :: error: assignment
        dnmABC = dnmsAB;

        dnmNone = dnmsBC;
        // :: error: assignment
        dnmA = dnmsBC;
        dnmB = dnmsBC;
        dnmC = dnmsBC;
        // :: error: assignment
        dnmAB = dnmsBC;
        // :: error: assignment
        dnmAC = dnmsBC;
        dnmBC = dnmsBC;
        // :: error: assignment
        dnmABC = dnmsBC;

        dnmNone = dnmsABC;
        dnmA = dnmsABC;
        dnmB = dnmsABC;
        dnmC = dnmsABC;
        dnmAB = dnmsABC;
        dnmAC = dnmsABC;
        dnmBC = dnmsABC;
        dnmABC = dnmsABC;
    }

    void stringToRegex1(@StringVal({"(a)"}) String a) {
        @DoesNotMatchRegex("(a)") String a2 = a;
        // :: error: assignment
        @DoesNotMatchRegex("\\(a\\)") String a3 = a;
    }
}
