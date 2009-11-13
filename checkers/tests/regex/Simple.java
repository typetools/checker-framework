import checkers.regex.quals.Regex;

import java.util.regex.Pattern;

public class Simple {

    void regString() {
        String s1 = "validRegex";
        String s2 = "(InvalidRegex";
    }

    void validRegString() {
        @Regex String s1 = "validRegex";
        @Regex String s2 = "(InvalidRegex";    // error
    }

    void compileCall() {
        Pattern.compile("test.*[^123]$");
        Pattern.compile("$test.*[^123");    // error
    }

    void requireValidReg(@Regex String reg, String nonReg) {
        Pattern.compile(reg);
        Pattern.compile(nonReg);    // error
    }

    void testAddition(@Regex String reg, String nonReg) {
        @Regex String s1 = reg;
        @Regex String s2 = reg + "d.*sf";
        @Regex String s3 = reg + reg;

        @Regex String n1 = nonReg;     // error
        @Regex String n2 = reg + "(df";    // error
        @Regex String n3 = reg + nonReg;   // error

        @Regex String o1 = nonReg;     // error
        @Regex String o2 = nonReg + "sdf";     // error
        @Regex String o3 = nonReg + reg;     // error

    }
}