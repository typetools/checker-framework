// Test case for issue #2334: http://tinyurl.com/cfissue/2334

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.dataflow.qual.Pure;

class Issue2334 {

    void m1(String stringFormal) {
        if (stringFormal.indexOf('d') != -1) {
            System.out.println("hey");
            @NonNegative int i = stringFormal.indexOf('d');
        }
    }

    String stringField;

    void m2() {
        if (stringField.indexOf('d') != -1) {
            System.out.println("hey");
            // :: error: (assignment.type.incompatible)
            @NonNegative int i = stringField.indexOf('d');
        }
    }

    public @Pure @Nullable Object getSuperclass() {
        return null;
    }

    void setSuperclass(@Nullable Object no) {
        // set the field returned by getSuperclass.
    }

    static void testInstanceofPositive3(PureTest pt) {
        if (!(pt.getSuperclass() instanceof Object)) {
            return;
        } else {
            pt.setSuperclass(null);
        }
        // :: error: (dereference.of.nullable)
        pt.getSuperclass().toString();
    }
}
