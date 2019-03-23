// Test case for issue #2334: http://tinyurl.com/cfissue/2334

import org.checkerframework.checker.index.qual.NonNegative;

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
}
