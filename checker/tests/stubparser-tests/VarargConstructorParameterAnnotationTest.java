import org.checkerframework.checker.nullness.qual.*;

/*
 * Tests parsing annotations on parameter represented by an array or vararg to the constructor.
 */
class ProcessBuilding2 {

    public void strArraysNonNull(@NonNull String[] untrustedArr) {
        new ProcessBuilder(untrustedArr);
    }

    public void strArraysNullable(@Nullable String[] untrustedArr) {
        //:: error: (argument.type.incompatible)
        new ProcessBuilder(untrustedArr);
    }

    public void strVarargNonNull(@NonNull String... untrustedArr) {
        new ProcessBuilder(untrustedArr);
    }

    public void strVarargNullable(@Nullable String... untrustedArr) {
        //:: error: (argument.type.incompatible)
        new ProcessBuilder(untrustedArr);
    }
}
