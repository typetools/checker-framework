
// No changes were needed to support this.

interface DefaultMethods {

    default String method(String param) {
        //:: error: (assignment.type.incompatible)
        param = null;

        String s = null;
        //:: error: (dereference.of.nullable)
        return s.toString();
    }
}
