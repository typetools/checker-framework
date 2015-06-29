
import org.checkerframework.checker.nullness.qual.*;

interface Function {
    String apply(String s);
}
interface Function2 {
    String apply(@Nullable String s);
}

class AssignmentContext {
    // Test assign
    Function f1 = String::toString;
    //:: error: (methodref.receiver.invalid)
    Function2 f2 = String::toString;

    // Test casts
    Object o1 = (Object) (Function) String::toString;
    //:: error: (methodref.receiver.invalid)
    Object o2 = (Object) (Function2) String::toString;

    void take(Function f) {
        // Test argument assingment
        take(String::toString);
    }
    void take2(Function2 f) {
        // Test argument assingment
        //:: error: (methodref.receiver.invalid)
        take2(String::toString);
    }

    Function supply() {
        // Test return assingment
        return String::toString;
    }
    Function2 supply2() {
        // Test return assingment
        //:: error: (methodref.receiver.invalid)
        return String::toString;
    }

}
