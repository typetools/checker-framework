import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.PolyNull;

// Test case for Issue602.java
// https://github.com/typetools/checker-framework/issues/602
// @skip-test
class Loop {
    @PolyNull
    String id(@PolyNull String o) {
        return o;
    }

    void loop(boolean condition) {
        @NonNull String notNull = "hello";
        String nullable = "";
        while (condition) {
            //:: error: (assignment.type.incompatible)
            notNull = nullable;
            //:: error: (assignment.type.incompatible)
            notNull = id(nullable);
            nullable = null;
        }
    }
}
