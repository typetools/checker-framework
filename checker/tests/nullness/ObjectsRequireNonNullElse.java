// Test case for https://tinyurl.com/cfissue/3056 and https://tinyurl.com/cfissue/3149 .
// Test case for https://tinyurl.com/cfissue/3149 .
// @below-java11-jdk-skip-test

import static java.util.Objects.requireNonNullElse;

import org.checkerframework.checker.nullness.qual.NonNull;

public class ObjectsRequireNonNullElse {
    public static void main(String[] args) {
        @NonNull String value = requireNonNullElse(null, "Something");
        System.err.println(requireNonNullElse(null, "Something"));

        // This should fail typechecks, because it fails at run time.
        // :: error: (argument.type.incompatible)
        System.err.println((Object) requireNonNullElse(null, null));
    }
}
