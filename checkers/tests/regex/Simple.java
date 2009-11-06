import checkers.regex.quals.ValidRegex;

import java.util.regex.Pattern;

public class Simple {

    void regString() {
        String s1 = "validRegex";
        String s2 = "(InvalidRegex";
    }

    void validRegString() {
        @ValidRegex String s1 = "validRegex";
        @ValidRegex String s2 = "(InvalidRegex";    // error
    }

    void compileCall() {
        Pattern.compile("test.*[^123]$");
        Pattern.compile("$test.*[^123");    // error
    }

    void requireValidReg(@ValidRegex String reg, String nonReg) {
        Pattern.compile(reg);
        Pattern.compile(nonReg);    // error
    }

    void testAddition(@ValidRegex String reg, String nonReg) {
        @ValidRegex String s1 = reg;
        @ValidRegex String s2 = reg + "d.*sf";
        @ValidRegex String s3 = reg + reg;

        @ValidRegex String n1 = nonReg;     // error
        @ValidRegex String n2 = reg + "(df";    // error
        @ValidRegex String n3 = reg + nonReg;   // error

        @ValidRegex String o1 = nonReg;     // error
        @ValidRegex String o2 = nonReg + "sdf";     // error
        @ValidRegex String o3 = nonReg + reg;     // error

    }
}