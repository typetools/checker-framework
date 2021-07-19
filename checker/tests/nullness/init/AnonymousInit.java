// Ensure field initialization checks for anonymous
// classes work.
public class AnonymousInit {
    Object o1 =
            new Object() {
                // :: error: (initialization.field.uninitialized)
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
