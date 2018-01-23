import org.checkerframework.checker.linear.qual.*;

public class UseUnusableVariable {

    public static void main(String[] args) {
        @Linear String s = "linear";

        // use up s
        String t = s.toUpperCase();

        // :: error: (use.unsafe)
        System.out.println(s.toLowerCase());

        System.out.println(t);
    }

}
