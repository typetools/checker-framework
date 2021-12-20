// @below-java14-jdk-skip-test
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class SwitchExpressionInvariant {
    public static boolean flag = false;

    void method(
            List<@NonNull String> nonnullStrings,
            List<@Nullable String> nullableStrings,
            int fenum) {

        List<@NonNull String> list =
                // :: error: (assignment.type.incompatible)
                switch (fenum) {
                        // :: error: (switch.expressioni.type.incompatible)
                    case 1 -> nonnullStrings;
                    default -> nullableStrings;
                };

        List<@Nullable String> list2 =
                switch (fenum) {
                        // :: error: (switch.expression.type.incompatible)
                    case 1 -> nonnullStrings;
                    default -> nullableStrings;
                };
    }
}
