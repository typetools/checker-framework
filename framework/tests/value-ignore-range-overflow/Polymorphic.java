import org.checkerframework.common.value.qual.*;

//@skip-test

class Polymorphic {

    //Identity functions

    int @PolyMinLen [] minlen_identity(int @PolyMinLen [] a) {
        return a;
    }

    // MinLen tests
    void minlen_id(int @MinLen(5) [] a) {
        int @MinLen(5) [] b = minlen_identity(a);
        //:: error: (assignment.type.incompatible)
        int @MinLen(6) [] c = minlen_identity(b);
    }
}
