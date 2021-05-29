// A test for the @MatchesRegex annotation.

import org.checkerframework.common.value.qual.*;

public class RegexMatching {
  void stringConstants() {
    @MatchesRegex("a*") String a = "a";
    @MatchesRegex("a*") String blank = "";
    // :: error: assignment
    @MatchesRegex("a*") String b = "b";

    // NOTE: these tests show that there are implicit anchors in the regular expressions
    // used by @MatchesRegex.
    // :: error: assignment
    @MatchesRegex("a*") String ab = "ab";
    // :: error: assignment
    @MatchesRegex("a*") String baa = "baa";

    @MatchesRegex("a") String a1 = "a";
    // :: error: assignment
    @MatchesRegex("a") String blank1 = "";
    // :: error: assignment
    @MatchesRegex("a") String b1 = "b";

    @MatchesRegex("\\s") String space = " ";
    @MatchesRegex("\\s+") String severalSpaces = "      ";
    // :: error: assignment
    @MatchesRegex("\\s") String b2 = "b";

    @MatchesRegex("[^abc]") String d = "d";
    @MatchesRegex("[^abc]") String d1 = String.valueOf(new char[] {'d'});
    // :: error: assignment
    @MatchesRegex("[^abc]") String c = "c";
  }

  void severalString(@StringVal({"a", "aa"}) String aaa, @StringVal({"aa", "b"}) String aab) {
    @MatchesRegex("a*") String a = aaa;
    // :: error: assignment
    @MatchesRegex("a*") String a1 = aab;

    @MatchesRegex("a+") String a2 = aaa;
    // :: error: assignment
    @MatchesRegex("a+") String a3 = aab;
  }

  void multipleRegexes(@StringVal({"a", "aa"}) String aaa, @StringVal({"aa", "b"}) String aab) {
    @MatchesRegex({"a*", "b*"}) String a = aaa;
    @MatchesRegex({"a*", "b*"}) String a1 = aab;

    // :: error: assignment
    @MatchesRegex({"aa", "b*"}) String a2 = aaa;
    @MatchesRegex({"aa", "b*"}) String a3 = aab;
  }

  void regexSubtypingConstant(@MatchesRegex({"a", "b"}) String ab) {
    // :: error: assignment
    @MatchesRegex("a") String a = ab;
    @MatchesRegex({"a", "b"}) String ab1 = ab;
    @MatchesRegex({"a", "b", "c"}) String abc = ab;
    // :: error: assignment
    @StringVal("a") String a1 = ab;
    // :: error: assignment
    @StringVal({"a", "b"}) String ab2 = ab;
    // :: error: assignment
    @StringVal({"a", "b", "c"}) String abc1 = ab;
  }

  void regexSubtyping2(@MatchesRegex({"a*", "b*"}) String ab) {
    // :: error: assignment
    @MatchesRegex("a*") String a = ab;
    @MatchesRegex({"a*", "b*"}) String ab1 = ab;
    @MatchesRegex({"a*", "b*", "c*"}) String abc = ab;
    // :: error: assignment
    @StringVal("a*") String a1 = ab;
    // :: error: assignment
    @StringVal({"a*", "b*"}) String ab2 = ab;
    // :: error: assignment
    @StringVal({"a*", "b*", "c*"}) String abc1 = ab;
  }

  void lubRegexes(
      @MatchesRegex({"a*"}) String astar, @MatchesRegex({"b*"}) String bstar, boolean b) {
    String s;
    if (b) {
      s = astar;
    } else {
      s = bstar;
    }
    @MatchesRegex({"a*", "b*"}) String s1 = s;
    // :: error: assignment
    @MatchesRegex({"a*"}) String s2 = s;
    // :: error: assignment
    @MatchesRegex({"b*"}) String s3 = s;
  }

  void lubRegexWithStringVal(
      @MatchesRegex({"a*"}) String astar, @StringVal({"b"}) String bval, boolean b) {
    String s;
    if (b) {
      s = astar;
    } else {
      s = bval;
    }
    // NOTE: This depends on the internal implementation.  Semantically identical code like this
    // yields an error:
    // @MatchesRegex({"a*", "b"}) String s0 = s;
    @MatchesRegex({"a*", "\\Qb\\E"}) String s1 = s;
    // :: error: assignment
    @MatchesRegex({"a*"}) String s2 = s;
    // :: error: assignment
    @StringVal({"b"}) String s3 = s;
    // :: error: assignment
    @MatchesRegex("b") String s4 = s;
    // :: error: assignment
    @MatchesRegex("^b$") String s5 = s;
  }
}
