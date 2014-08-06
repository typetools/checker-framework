// Make sure that we actually receive errors from sub-checkers.
import org.checkerframework.checker.regex.qual.Regex;

class NullnessAndRegex {
    //:: error: (assignment.type.incompatible)
    @Regex String s1 = "De(mo";
    //:: error: (assignment.type.incompatible)
    Object f = null;
    //:: error: (assignment.type.incompatible)
    @Regex String s2 = "De(mo";
}
