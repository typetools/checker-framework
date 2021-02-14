import org.checkerframework.checker.initialization.qual.Initialized;
import org.checkerframework.checker.initialization.qual.NotOnlyInitialized;
import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.dataflow.qual.Pure;

public class SimpleFbc {

    SimpleFbc f;
    @NotOnlyInitialized SimpleFbc g;

    @Pure
    int pure() {
        return 1;
    }

    // :: error: (initialization.fields.uninitialized)
    public SimpleFbc(String arg) {}

    void test() {
        @NonNull String s = "234";

        // :: error: (assignment.type.incompatible)
        s = null;
        System.out.println(s);
    }

    void test2(@UnknownInitialization @NonNull SimpleFbc t) {
        // :: error: (assignment.type.incompatible)
        @NonNull SimpleFbc a = t.f;
    }

    // check initialized-only semantics for fields
    void test3(@UnknownInitialization @NonNull SimpleFbc t) {
        @Initialized @Nullable SimpleFbc a = t.f;

        // :: error: (assignment.type.incompatible)
        @Initialized @Nullable SimpleFbc b = t.g;
    }

    void simplestTestEver() {
        @NonNull String a = "abc";

        // :: error: (assignment.type.incompatible)
        a = null;

        // :: error: (assignment.type.incompatible)
        @NonNull String b = null;
    }

    void anotherMethod() {
        @Nullable String s = null;

        @Initialized @Nullable String t = s;
    }
}
