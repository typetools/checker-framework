// Test case for issue #1248:
// https://github.com/typetools/checker-framework/issues/1248

import java.util.function.Predicate;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class RefinedLocalInLambda {

    public static void main(String[] args) {
        printIntegersGreaterThan(10);
    }

    public static void printIntegersGreaterThan(@Nullable Integer limit) {
        // :: error: (unboxing.of.nullable)
        printIntegersWithPredicate(i -> i > limit); // type-checking fails
        if (limit == null) {
            return;
        }
        printIntegersWithPredicate(i -> i > limit); // type-checking succeeds
        @NonNull Integer limit2 = limit;
        printIntegersWithPredicate(i -> i > limit2); // type-checking succeeds
        Integer limit3 = limit;
        printIntegersWithPredicate(i -> i > limit3); // type-checking succeeds
    }

    public static void printIntegersWithPredicate(Predicate<Integer> tester) {
        for (int i = 0; i < 100; i++) {
            if (tester.test(i)) {
                System.out.println(i);
            }
        }
    }
}
