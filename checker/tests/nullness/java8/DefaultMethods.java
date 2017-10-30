interface DefaultMethods {

    default void method(String param) {
        // :: error: (assignment.type.incompatible)
        param = null;

        String s = null;
        // :: error: (dereference.of.nullable)
        s.toString();

        // Ensure dataflow is running
        s = "";
        s.toString();
    }
}
