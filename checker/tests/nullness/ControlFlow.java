import org.checkerframework.checker.nullness.qual.*;

// test-case for issue 160
public class ControlFlow {
    public static void main(String[] args) {
        String s = null;
        if (s == null) {
            // Important!
        } else {
            // Can also throw exception or call System#exit
            return;
        }
        // :: error: (dereference.of.nullable)
        System.out.println(s.toString());
    }
}
