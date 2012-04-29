import java.util.regex.Matcher;
import java.util.regex.Pattern;

import checkers.regex.RegexUtil;
import checkers.regex.quals.Regex;

public class GroupCounts {
    void testGroupCount() {
        @Regex(0) String s1 = "abc";
        @Regex(1) String s2 = "(abc)";
        @Regex(2) String s3 = "()(abc)";
        @Regex(3) String s4 = "(abc())()";
        @Regex(4) String s5 = "((((abc))))";

        @Regex(0) String s7 = "(abc)";
        @Regex String s9 = "()()(())";
        @Regex(2) String s10 = "()()(())";
        @Regex(3) String s11 = "()()(())";

        //:: error: (assignment.type.incompatible)
        @Regex(2) String s6 = "nonregex(";    // error
        //:: error: (assignment.type.incompatible)
        @Regex(1) String s8 = "abc";    // error
        //:: error: (assignment.type.incompatible)
        @Regex(3) String s12 = "()()";    // error
        //:: error: (assignment.type.incompatible)
        @Regex(4) String s13 = "(())()";    // error
    }

    void testPatternCompileGroupCount(@Regex String r, @Regex(3) String r3, @Regex(5) String r5) {
        @Regex(5) Pattern p1 = Pattern.compile(r5);
        @Regex Pattern p2 = Pattern.compile(r5);
        @Regex Pattern p3 = Pattern.compile(r);

        //:: error: (assignment.type.incompatible)
        @Regex(6) Pattern p4 = Pattern.compile(r5);   // error
        //:: error: (assignment.type.incompatible)
        @Regex(6) Pattern p5 = Pattern.compile(r3);   // error

        // Make sure Pattern.compile still works when passed an @Unqualified String
        // that's actually a regex, with the warning suppressed.
        @SuppressWarnings("regex:argument.type.incompatible")
        Pattern p6 = Pattern.compile("(" + r + ")");
    }

    void testConcatenationGroupCount(@Regex String r, @Regex(3) String r3, @Regex(5) String r5) {
        @Regex(0) String s1 = r + r;
        @Regex(3) String s2 = r + r3;
        @Regex(8) String s3 = r3 + r5;

        //:: error: (assignment.type.incompatible)
        @Regex(1) String s4 = r + r;
        //:: error: (assignment.type.incompatible)
        @Regex(4) String s5 = r + r3;
        //:: error: (assignment.type.incompatible)
        @Regex(9) String s6 = r3 + r5;
    }

    void testCompoundConcatenationWithGroups(@Regex String s0, @Regex(1) String s1, @Regex(3) String s3) {
        s0 += s0;
        @Regex String test0 = s0;
        //:: error: (assignment.type.incompatible)
        @Regex(1) String test01 = s0;

        s0 += s1;
        @Regex(1) String test1 = s0;
        //:: error: (assignment.type.incompatible)
        @Regex(2) String test12 = s0;

        s1 += s3;
        @Regex(4) String test4 = s1;
        //:: error: (assignment.type.incompatible)
        @Regex(5) String test45 = s1;
    }

    void testAsRegexGroupCounts(String s) {
        @Regex String test1 = RegexUtil.asRegex(s);
        //:: error: (assignment.type.incompatible)
        @Regex(1) String test2 = RegexUtil.asRegex(s);

        @Regex(3) String test3 = RegexUtil.asRegex(s, 3);
        //:: error: (assignment.type.incompatible)
        @Regex(4) String test4 = RegexUtil.asRegex(s, 3);
    }

    void testMatcherGroupCounts(@Regex Matcher m0, @Regex(1) Matcher m1, @Regex(4) Matcher m4, int n) {
        m0.end(0);
        m0.group(0);
        m0.start(0);

        //:: error: (group.count.invalid)
        m0.end(1);
        //:: error: (group.count.invalid)
        m0.group(1);
        //:: error: (group.count.invalid)
        m0.start(1);

        m1.start(0);
        m1.start(1);

        //:: error: (group.count.invalid)
        m1.start(2);

        m4.start(0);
        m4.start(2);
        m4.start(4);

        //:: error: (group.count.invalid)
        m4.start(5);

        //:: warning: (group.count.unknown)
        m0.start(n);
    }
}
