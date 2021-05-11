import java.util.regex.Pattern;
import org.checkerframework.checker.regex.qual.Regex;

public class SimpleRegex {

  void regString() {
    String s1 = "validRegex";
    String s2 = "(InvalidRegex";
  }

  void validRegString() {
    @Regex String s1 = "validRegex";
    // :: error: (assignment)
    @Regex String s2 = "(InvalidRegex"; // error
  }

  void compileCall() {
    Pattern.compile("test.*[^123]$");
    // :: error: (argument)
    Pattern.compile("$test.*[^123"); // error
  }

  void requireValidReg(@Regex String reg, String nonReg) {
    Pattern.compile(reg);
    // :: error: (argument)
    Pattern.compile(nonReg); // error
  }

  void testAddition(@Regex String reg, String nonReg) {
    @Regex String s1 = reg;
    @Regex String s2 = reg + "d.*sf";
    @Regex String s3 = reg + reg;

    // :: error: (assignment)
    @Regex String n1 = nonReg; // error
    // :: error: (assignment)
    @Regex String n2 = reg + "(df"; // error
    // :: error: (assignment)
    @Regex String n3 = reg + nonReg; // error

    // :: error: (assignment)
    @Regex String o1 = nonReg; // error
    // :: error: (assignment)
    @Regex String o2 = nonReg + "sdf"; // error
    // :: error: (assignment)
    @Regex String o3 = nonReg + reg; // error
  }

  @Regex String regex = "()";
  String nonRegex = "()";

  void testCompoundConcatenation() {
    takesRegex(regex);
    // :: error: (compound.assignment)
    regex += ")"; // error
    takesRegex(regex);

    nonRegex = "()";
    // nonRegex is refined by flow to be a regular expression
    takesRegex(nonRegex);
    nonRegex += ")";
    // :: error: (argument)
    takesRegex(nonRegex); // error
  }

  void takesRegex(@Regex String s) {}

  void testChar() {
    @Regex char c1 = 'c';
    @Regex Character c2 = 'c';

    // :: error: (assignment)
    @Regex char c3 = '('; // error
    // :: error: (assignment)
    @Regex Character c4 = '('; // error
  }

  void testCharConcatenation() {
    @Regex String s1 = "rege" + 'x';
    @Regex String s2 = 'r' + "egex";

    // :: error: (assignment)
    @Regex String s4 = "rege" + '('; // error
    // :: error: (assignment)
    @Regex String s5 = "reg(" + 'x'; // error
    // :: error: (assignment)
    @Regex String s6 = '(' + "egex"; // error
    // :: error: (assignment)
    @Regex String s7 = 'r' + "ege("; // error
  }

  void testPatternLiteral() {
    Pattern.compile("non(", Pattern.LITERAL);
    Pattern.compile(foo("regex"), Pattern.LITERAL);

    // :: error: (argument)
    Pattern.compile(foo("regex("), Pattern.LITERAL); // error
    // :: error: (argument)
    Pattern.compile("non("); // error
    // :: error: (argument)
    Pattern.compile(foo("regex")); // error
    // :: error: (argument)
    Pattern.compile("non(", Pattern.CASE_INSENSITIVE); // error
  }

  public static String foo(@Regex String s) {
    return "non((";
  }

  //    TODO: This is not supported until the framework can read explicit
  //    annotations from arrays.
  //    void testArrayAllowedTypes() {
  //        @Regex char[] ca1;
  //        char @Regex [] ca2;
  //        @Regex char @Regex [] ca3;
  //        @Regex String[] s1;
  //
  //        // :: error: (type.invalid)
  //        @Regex double[] da1;   // error
  //        // :: error: (type.invalid)
  //        double @Regex [] da2;   // error
  //        // :: error: (type.invalid)
  //        @Regex double @Regex [] da3;   // error
  //        // :: error: (type.invalid)
  //        String @Regex [] s2;    // error
  //    }

  //    TODO: This is not supported until the Regex Checker supports flow
  //    sensitivity. See the associated comment at
  //    org.checkerframework.checker/regex/RegexAnnotatedTypeFactory.java:visitNewArray
  //    void testCharArrays(char c, @Regex char r) {
  //        char @Regex [] c1 = {'r', 'e', 'g', 'e', 'x'};
  //        char @Regex [] c2 = {'(', 'r', 'e', 'g', 'e', 'x', ')', '.', '*'};
  //        char @Regex [] c3 = {r, 'e', 'g', 'e', 'x'};
  //
  //        // :: error: (assignment)
  //        char @Regex [] c4 = {'(', 'r', 'e', 'g', 'e', 'x'};   // error
  //        // :: error: (assignment)
  //        char @Regex [] c5 = {c, '.', '*'};   // error
  //    }
}
