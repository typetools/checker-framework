import checkers.regex.quals.*;

// test-case for issue 128
public class TestRegex {
   
    public void Concatenation2() {
        @Regex String a = "a";
        //:: error: (compound.assignment.type.incompatible)
        a += "(";
    }
}
