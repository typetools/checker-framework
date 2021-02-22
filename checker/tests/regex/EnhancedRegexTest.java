import org.checkerframework.checker.regex.qual.EnhancedRegex;

public class EnhancedRegexTest {
    void fun() {
        System.out.println("Does this run? If you see this you know.");
        // legal
        @EnhancedRegex String regexp1 = "(a?).(abc)";
        // legal
        @EnhancedRegex({0, 2})
        String regexp2 = "(a?).(abc)";
        // legal
        @EnhancedRegex({0, 1, 2, 2})
        String regexp3 = "(a?).(abc)";
        @EnhancedRegex({0, 1, 2, 3})
        // :: error: (assignment.type.incompatible)
        String regexp4 = "(a?).(abc)";
        // legal
        @EnhancedRegex({0, 2, 2})
        String regexp5 = "(a)?(abc)";
        @EnhancedRegex({0, 1, 2, 2})
        // :: error: (assignment.type.incompatible)
        String regexp6 = "(a)?(abc)";
    }
}
