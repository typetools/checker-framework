interface FunctionAC {
    String apply(String s);
}

class AssignmentContext {
    // Test assign
    FunctionAC f1 = String::toString;

    // Test casts
    Object o1 = (Object) (FunctionAC) String::toString;

    void take(FunctionAC f) {
        // Test argument assingment
        take(String::toString);
    }

    FunctionAC supply() {
        // Test return assingment
        return String::toString;
    }
}
