// Ensure field initialization checks for anonymous
// classes work.
class AnonymousInit {
    // :: error: (initialization.fields.uninitialized)
    Object o1 =
            new Object() {
                Object s;

                public String toString() {
                    return s.toString();
                }
            };
    Object o2 =
            new Object() {
                Object s = "hi";

                public String toString() {
                    return s.toString();
                }
            };
}
