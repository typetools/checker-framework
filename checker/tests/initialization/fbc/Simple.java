
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.checker.initialization.qual.*;
import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.*;

public class Simple {

    Simple f;
    @NotOnlyInitialized Simple g;
    
    @Pure int pure() {
        return 1;
    }

    //:: error: (initialization.fields.uninitialized)
    public Simple(String arg) {
    }

    void test() {
        @NonNull String s = "234";

        //:: error: (assignment.type.incompatible)
        s = null;
        System.out.println(s);
    }

    void test2(@UnknownInitialization @NonNull Simple t) {
        //:: error: (assignment.type.incompatible)
        @NonNull Simple a = t.f;
    }

    // check committed-only semantics for fields
    void test3(@UnknownInitialization @NonNull Simple t) {
        @Initialized @Nullable Simple a = t.f;

        //:: error: (assignment.type.incompatible)
        @Initialized @Nullable Simple b = t.g;
    }

    void simplestTestEver() {
        @NonNull String a = "abc";

        //:: error: (assignment.type.incompatible)
        a = null;

        //:: error: (assignment.type.incompatible)
        @NonNull String b = null;
    }

    void anotherMethod() {
        @Nullable String s = null;

        @Initialized @Nullable String t = s;
    }
}
