
import checkers.initialization.quals.*;
import checkers.nonnull.quals.*;
import checkers.quals.*;

public class Simple {

    Simple f;
    @NotOnlyCommitted Simple g;
    
    @Pure int pure() {
        return 1;
    }

    //:: error: (commitment.fields.uninitialized)
    public Simple(String arg) {
    }

    void test() {
        @NonNull String s = "234";

        //:: error: (assignment.type.incompatible)
        s = null;
        System.out.println(s);
    }

    void test2(@Unclassified @NonNull Simple t) {
        //:: error: (assignment.type.incompatible)
        @NonNull Simple a = t.f;
    }

    // check committed-only semantics for fields
    void test3(@Unclassified @NonNull Simple t) {
        @Committed @Nullable Simple a = t.f;

        //:: error: (assignment.type.incompatible)
        @Committed @Nullable Simple b = t.g;
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

        @Committed @Nullable String t = s;
    }
}
