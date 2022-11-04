// A test for the @DoesNotMatchRegex annotation.

import org.checkerframework.common.value.qual.*;

public class RegexNonMatching {

    void stringConstants() {
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("a*") String aStar = "a";
        // :: error: assignment.type.incompatible
        aStar = "";
        aStar = "b";
        @DoesNotMatchRegex({"a+"}) String aPlus = "b";
        // @DoesNotMatchRegex("a+") String aPlus = "b";
        aStar = "ab";
        aStar = "baa";

        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("a") String a1 = "a";
        @DoesNotMatchRegex("a") String blank1 = "";
        @DoesNotMatchRegex("a") String b1 = "b";

        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("\\s") String space = " ";
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("\\s+") String severalSpaces = "      ";
        // TODO: this should work
        @DoesNotMatchRegex("\\s") String b2 = "b";

        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("[^abc]") String d = "d";
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("[^abc]") String d1 = String.valueOf(new char[] {'d'});
        // TODO: this should work, shouldn't it?
        @DoesNotMatchRegex("[^abc]") String c = "c";
    }

    void severalString(@StringVal({"a", "aa"}) String aaa, @StringVal({"aa", "b"}) String aab) {
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("a*") String a = aaa;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("a*") String a1 = aab;

        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("a+") String a2 = aaa;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("a+") String a3 = aab;
    }

    void multipleRegexes(@StringVal({"a", "aa"}) String aaa, @StringVal({"aa", "b"}) String aab) {
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"a*", "b*"}) String a = aaa;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"a*", "b*"}) String a1 = aab;

        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"aa", "b*"}) String a2 = aaa;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"aa", "b*"}) String a3 = aab;
    }

    void regexSubtypingConstant(@DoesNotMatchRegex({"a", "b"}) String ab) {
        @DoesNotMatchRegex("a") String a = ab;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("c") String c = ab;
        @DoesNotMatchRegex({"a", "b"}) String ab1 = ab;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"a", "b", "c"}) String abc = ab;
        // :: error: assignment.type.incompatible
        @StringVal("a") String a1 = ab;
        // :: error: assignment.type.incompatible
        @StringVal({"a", "b"}) String ab2 = ab;
        // :: error: assignment.type.incompatible
        @StringVal({"a", "b", "c"}) String abc1 = ab;
    }

    void regexSubtyping2(@DoesNotMatchRegex({"a*", "b*"}) String ab) {
        @DoesNotMatchRegex("a*") String a = ab;
        @DoesNotMatchRegex({"a*", "b*"}) String ab1 = ab;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"a*", "b*", "c*"}) String abc = ab;
        // :: error: assignment.type.incompatible
        @StringVal("a*") String a1 = ab;
        // :: error: assignment.type.incompatible
        @StringVal({"a*", "b*"}) String ab2 = ab;
        // :: error: assignment.type.incompatible
        @StringVal({"a*", "b*", "c*"}) String abc1 = ab;
        // :: error: assignment.type.incompatible
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
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"a*", "b*"}) String s1 = s;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"a*"}) String s2 = s;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({"b*"}) String s3 = s;
        s3 = s1;
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex({}) String s4 = s;
        // :: error: assignment.type.incompatible
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
        // :: error: assignment.type.incompatible
        dnmA = dnmsNone;
        // :: error: assignment.type.incompatible
        dnmB = dnmsNone;
        // :: error: assignment.type.incompatible
        dnmC = dnmsNone;
        // :: error: assignment.type.incompatible
        dnmAB = dnmsNone;
        // :: error: assignment.type.incompatible
        dnmAC = dnmsNone;
        // :: error: assignment.type.incompatible
        dnmBC = dnmsNone;
        // :: error: assignment.type.incompatible
        dnmABC = dnmsNone;

        dnmNone = dnmsA;
        dnmA = dnmsA;
        // :: error: assignment.type.incompatible
        dnmB = dnmsA;
        // :: error: assignment.type.incompatible
        dnmC = dnmsA;
        // :: error: assignment.type.incompatible
        dnmAB = dnmsA;
        // :: error: assignment.type.incompatible
        dnmAC = dnmsA;
        // :: error: assignment.type.incompatible
        dnmBC = dnmsA;
        // :: error: assignment.type.incompatible
        dnmABC = dnmsA;

        dnmNone = dnmsB;
        // :: error: assignment.type.incompatible
        dnmA = dnmsB;
        dnmB = dnmsB;
        // :: error: assignment.type.incompatible
        dnmC = dnmsB;
        // :: error: assignment.type.incompatible
        dnmAB = dnmsB;
        // :: error: assignment.type.incompatible
        dnmAC = dnmsB;
        // :: error: assignment.type.incompatible
        dnmBC = dnmsB;
        // :: error: assignment.type.incompatible
        dnmABC = dnmsB;

        dnmNone = dnmsC;
        // :: error: assignment.type.incompatible
        dnmA = dnmsC;
        // :: error: assignment.type.incompatible
        dnmB = dnmsC;
        dnmC = dnmsC;
        // :: error: assignment.type.incompatible
        dnmAB = dnmsC;
        // :: error: assignment.type.incompatible
        dnmAC = dnmsC;
        // :: error: assignment.type.incompatible
        dnmBC = dnmsC;
        // :: error: assignment.type.incompatible
        dnmABC = dnmsC;

        dnmNone = dnmsAC;
        dnmA = dnmsAC;
        // :: error: assignment.type.incompatible
        dnmB = dnmsAC;
        dnmC = dnmsAC;
        // :: error: assignment.type.incompatible
        dnmAB = dnmsAC;
        dnmAC = dnmsAC;
        // :: error: assignment.type.incompatible
        dnmBC = dnmsAC;
        // :: error: assignment.type.incompatible
        dnmABC = dnmsAC;

        dnmNone = dnmsAB;
        dnmA = dnmsAB;
        dnmB = dnmsAB;
        // :: error: assignment.type.incompatible
        dnmC = dnmsAB;
        dnmAB = dnmsAB;
        // :: error: assignment.type.incompatible
        dnmAC = dnmsAB;
        // :: error: assignment.type.incompatible
        dnmBC = dnmsAB;
        // :: error: assignment.type.incompatible
        dnmABC = dnmsAB;

        dnmNone = dnmsBC;
        // :: error: assignment.type.incompatible
        dnmA = dnmsBC;
        dnmB = dnmsBC;
        dnmC = dnmsBC;
        // :: error: assignment.type.incompatible
        dnmAB = dnmsBC;
        // :: error: assignment.type.incompatible
        dnmAC = dnmsBC;
        dnmBC = dnmsBC;
        // :: error: assignment.type.incompatible
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
        // :: error: assignment.type.incompatible
        @DoesNotMatchRegex("\\(a\\)") String a3 = a;
    }
}
