// Ensure field initialization checks for anonymous
// classes work.
class AnonymousInit {
    Object o1 =
            // :: error: (initialization.fields.uninitialized)
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
