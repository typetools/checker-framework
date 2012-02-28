import checkers.regex.quals.Regex;

public class LUB {

    void test1(@Regex(4) String s4, boolean b) {
        String s = null;
        if (b) {
            s = s4;
        }
        @Regex(4) String test = s;

        //:: error: (assignment.type.incompatible)
        @Regex(5) String test2 = s;
    }

    void test2(@Regex(2) String s2, @Regex(4) String s4, boolean b) {
        String s = s4;
        if (b) {
            s = s2;
        }
        @Regex(2) String test = s;

        //:: error: (assignment.type.incompatible)
        @Regex(3) String test2 = s;
    }
}
