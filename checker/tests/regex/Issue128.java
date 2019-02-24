// test-case for issue 128

import org.checkerframework.checker.regex.qual.*;

public class Issue128 {

    public void Concatenation2() {
        @Regex String a = "a";
        // :: error: (compound.assignment.type.incompatible)
        a += "(";
    }
}
