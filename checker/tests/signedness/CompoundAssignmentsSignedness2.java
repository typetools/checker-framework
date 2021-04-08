// Test case for issue #3709: https://github.com/typetools/checker-framework/issues/3709

public class CompoundAssignmentsSignedness2 {
  void additionWithCompoundAssignment(char c, int i1) {
    i1 += c;
  }

  void additionWithoutCompoundAssignment1(char c, int i1) {
    i1 = (int) (i1 + c);
  }

  void additionWithoutCompoundAssignment2(char c, int i1) {
    i1 = i1 + c;
  }

  void subtractionWithCompoundAssignment(char c, int i1) {
    i1 -= c;
  }

  void subtractionWithoutCompoundAssignment1(char c, int i1) {
    i1 = (int) (i1 - c);
  }

  void subtractionWithoutCompoundAssignment2(char c, int i1) {
    i1 = i1 - c;
  }

  void multiplicationWithCompoundAssignment(char c, int i1) {
    i1 *= c;
  }

  void multiplicationWithoutCompoundAssignment1(char c, int i1) {
    i1 = (int) (i1 * c);
  }

  void multiplicationWithoutCompoundAssignment2(char c, int i1) {
    i1 = i1 * c;
  }

  void divisionWithCompoundAssignment(char c, int i1) {
    i1 /= c;
  }

  void divisionWithoutCompoundAssignment1(char c, int i1) {
    i1 = (int) (i1 / c);
  }

  void divisionWithoutCompoundAssignment2(char c, int i1) {
    i1 = i1 / c;
  }

  void modulusWithCompoundAssignment(char c, int i1) {
    i1 %= c;
  }

  void modulusWithoutCompoundAssignment1(char c, int i1) {
    i1 = (int) (i1 % c);
  }

  void modulusWithoutCompoundAssignment2(char c, int i1) {
    i1 = i1 % c;
  }
}
