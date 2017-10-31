/** Tests that a final field annotation is inferred */
class FieldsImplicits {
    final String finalField = "asdf";
    static final String finalStaticField = "asdf";
    String nonFinalField = "asdf";

    void test() {
        boolean a = finalField == "asdf";
        boolean b = finalStaticField == "asdf";
        // :: error: (not.interned)
        boolean c = nonFinalField == "asdf";
    }
}
