import org.checkerframework.common.value.qual.*;

//@skip-test until PolyValue is added (https://github.com/typetools/checker-framework/issues/1236)

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
