import java.lang.SuppressWarnings;

interface Function {
    String apply(String s);
}

@SuppressWarnings("oigj")
class AssignmentContext {
    // Test assign
    Function f1 = String::toString;

    // Test casts
    Object o1 = (Object) (Function) String::toString;

    void take(Function f) {
        // Test argument assingment
        take(String::toString);
    }

    Function supply() {
        // Test return assingment
        return String::toString;
    }
}
