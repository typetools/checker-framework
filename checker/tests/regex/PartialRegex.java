import org.checkerframework.checker.regex.qual.Regex;

public class PartialRegex {
    void m(@Regex String re, String non) {
        String l = "(";
        String r = ")";

        @Regex String test1 = l + r;
        @Regex String test2 = l + re + r;
        @Regex String test3 = l + r + l + r;
        @Regex String test4 = l + l + r + r;
        @Regex String test5 = l + l + re + r + r;

        // :: error: (assignment.type.incompatible)
        @Regex String fail1 = r + l;
        // :: error: (assignment.type.incompatible)
        @Regex String fail2 = r + non + l;
        // :: error: (assignment.type.incompatible)
        @Regex String fail3 = l + r + r;
    }
}
