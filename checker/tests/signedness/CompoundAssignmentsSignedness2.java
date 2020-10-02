// Test case for issue #3709: https://github.com/typetools/checker-framework/issues/3709

public class CompoundAssignmentsSignedness2 {
    void additionWithCompoundAssignment(char c, int i1) {
        i1 += c;
    }

    void additionWithoutCompoundAssignment(char c, int i1) {
        i1 = (int) (i1 + c);
    }

    void subtractionWithCompoundAssignment(char c, int i1) {
        i1 -= c;
    }

    void subtractionWithoutCompoundAssignment(char c, int i1) {
        i1 = (int) (i1 - c);
    }

    void multiplicationWithCompoundAssignment(char c, int i1) {
        i1 *= c;
    }

    void multiplicationWithoutCompoundAssignment(char c, int i1) {
        i1 = (int) (i1 * c);
    }

    void divisionWithCompoundAssignment(char c, int i1) {
        i1 /= c;
    }

    void divisionWithoutCompoundAssignment(char c, int i1) {
        i1 = (int) (i1 / c);
    }

    void modulusWithCompoundAssignment(char c, int i1) {
        i1 %= c;
    }

    void modulusWithoutCompoundAssignment(char c, int i1) {
        i1 = (int) (i1 % c);
    }
}
