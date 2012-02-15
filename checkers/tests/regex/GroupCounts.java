import java.util.regex.Pattern;

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
}
