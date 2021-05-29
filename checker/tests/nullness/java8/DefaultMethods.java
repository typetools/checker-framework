interface DefaultMethods {

  default void method(String param) {
    // :: error: (assignment)
    param = null;

    String s = null;
    // :: error: (dereference.of.nullable)
    s.toString();

    // Ensure dataflow is running
    s = "";
    s.toString();
  }
}
