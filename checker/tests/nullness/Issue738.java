import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

// Testcase for #738
// https://github.com/typetools/checker-framework/issues/738
// Also, see framework/tests/all-systems/Issue738.java
public class Issue738 {
    void methodA(int[] is, Object @Nullable [] os, int i) {
        // The type argument to methodB* for each call below is Cloneable & Serializable

        // NullnessTransfer changes the type of an argument that is assigned to a @NonNull parameter
        // to @NonNull. Use a switch statement to prevent this.
        switch (i) {
            case 1:
                methodB(is, os);
                break;
            case 2:
                // :: error: (argument.type.incompatible)
                methodB2(is, os);
                break;
            case 3:
                // :: error: (type.argument.type.incompatible)
                methodB3(is, os);
                break;
            case 4:
                // :: error: (type.argument.type.incompatible)
                methodB4(is, os);
                break;
        }
    }

    <T> void methodB(T paramA, T paramB) {}

    <T> void methodB2(T paramA, @NonNull T paramB) {}

    <@NonNull T extends @NonNull Object> void methodB3(T paramA, T paramB) {}

    <T extends @NonNull Cloneable> void methodB4(T paramA, T paramB) {}
}
