import checkers.regex.quals.Regex;

import java.util.regex.Pattern;

public class Simple {

    void regString() {
        String s1 = "validRegex";
        String s2 = "(InvalidRegex";
    }

    void validRegString() {
        @Regex String s1 = "validRegex";
        //:: (assignment.type.incompatible)
        @Regex String s2 = "(InvalidRegex";    // error
    }

    void compileCall() {
        Pattern.compile("test.*[^123]$");
        //:: (argument.type.incompatible)
        Pattern.compile("$test.*[^123");    // error
    }

    void requireValidReg(@Regex String reg, String nonReg) {
        Pattern.compile(reg);
        //:: (argument.type.incompatible)
        Pattern.compile(nonReg);    // error
    }

    void testAddition(@Regex String reg, String nonReg) {
        @Regex String s1 = reg;
        @Regex String s2 = reg + "d.*sf";
        @Regex String s3 = reg + reg;

        //:: (assignment.type.incompatible)
        @Regex String n1 = nonReg;     // error
        //:: (assignment.type.incompatible)
        @Regex String n2 = reg + "(df";    // error
        //:: (assignment.type.incompatible)
        @Regex String n3 = reg + nonReg;   // error

        //:: (assignment.type.incompatible)
        @Regex String o1 = nonReg;     // error
        //:: (assignment.type.incompatible)
        @Regex String o2 = nonReg + "sdf";     // error
        //:: (assignment.type.incompatible)
        @Regex String o3 = nonReg + reg;     // error

    }
}
