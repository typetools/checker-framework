import org.checkerframework.checker.regex.qual.*;

// test-case for issue 128
public class TestRegex {

    public void Concatenation2() {
        @Regex String a = "a";
        // :: error: (compound.assignment.type.incompatible)
        a += "(";
    }
}

// test-case for issue 148
class Search {
    public static void main(String[] args) {
        if (!org.checkerframework.checker.regex.util.RegexUtil.isRegex(args[0], 4)) {
            return;
        }
        @Regex(4) String regex = args[0];
    }
}
