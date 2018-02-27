import java.util.regex.*;

/**
 * Call this program with two arguments; a regular expression and a string. The program prints the
 * text, from the string, that matches the first capturing group in the regular expression.
 */
public class RegexExample {
    public static void main(String[] args) {
        String regex = args[0];
        String content = args[1];

        Pattern pat = Pattern.compile(regex);
        Matcher mat = pat.matcher(content);

        if (mat.matches()) {
            System.out.println("Group 1: " + mat.group(1));
        } else {
            System.out.println("No match!");
        }
    }
}
