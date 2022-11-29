import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

class EnsuresQualifierTest {

  @AinferTop int field1;
  @AinferTop int field2;

  @AinferTop int top;
  @AinferParent int parent;
  @AinferSibling1 int sibling1;
  @AinferSibling2 int sibling2;
  @AinferBottom int bottom;

  void field1IsParent() {
    field1 = parent;
  }

  void field1IsParent_2(boolean b) {
    if (b) {
      field1 = sibling1;
    } else {
      field1 = sibling2;
    }
  }

  void field1IsAinferSibling2() {
    field1 = sibling2;
  }

  void field1IsAinferSibling2_2(boolean b) {
    if (b) {
      field1 = sibling2;
    } else {
      field1 = bottom;
    }
  }

  void parentIsAinferSibling1() {
    parent = sibling1;
  }

  // Prevent refinement of the `parent` field variable.
  void parentIsParent(@AinferParent int x) {
    parent = x;
  }

  void noEnsures() {}

  void client1() {
    field1IsParent();
    // :: warning: (assignment)
    @AinferParent int p = field1;
  }

  void client2() {
    field1IsParent_2(true);
    // :: warning: (assignment)
    @AinferParent int p = field1;
  }

  void client3() {
    field1IsAinferSibling2();
    // :: warning: (assignment)
    @AinferSibling2 int x = field1;
  }

  void client4() {
    field1IsAinferSibling2_2(true);
    // :: warning: (assignment)
    @AinferSibling2 int x = field1;
  }

  void client5() {
    parentIsAinferSibling1();
    // :: warning: (assignment)
    @AinferSibling1 int x = parent;
  }
}
