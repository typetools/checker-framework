// @below-java17-jdk-skip-test
import org.checkerframework.checker.nullness.qual.Nullable;

public class NullnessSwitchStatementRules {
    @Nullable Object field = null;

    void method(int selector) {
        field = new Object();
        switch (selector) {
            case 1 -> field = null;
            case 2 -> field.toString();
        }

        field = new Object();
        switch (selector) {
            case 1 -> {
                field = null;
            }
            case 2 -> {
                field.toString();
            }
        }

        field = new Object();
        switch (selector) {
            case 1 -> {
                field = null;
            }
            case 2 -> {
                field.toString();
            }
        }

        field = new Object();
        switch (selector) {
            case 1:
                field = null;
            case 2:
                // :: error: (dereference.of.nullable)
                field.toString();
        }
    }
}
