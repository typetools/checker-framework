import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferTop;

class RequiresQualifierTest {

  @AinferTop int field1;
  @AinferTop int field2;

  @AinferTop int top;
  @AinferParent int parent;
  @AinferSibling1 int sibling1;
  @AinferSibling2 int sibling2;
  @AinferBottom int bottom;

  void field1IsParent() {
    // :: warning: (assignment)
    @AinferParent int x = field1;
  }

  void field1IsAinferSibling2() {
    // :: warning: (assignment)
    @AinferSibling2 int x = field1;
  }

  void parentIsAinferSibling1() {
    // :: warning: (assignment)
    @AinferSibling1 int x = parent;
  }

  void noRequirements() {}

  void client2(@AinferParent int p) {
    field1 = p;
    field1IsParent();
  }

  void client1() {
    noRequirements();

    field1 = parent;
    field1IsParent();

    field1 = sibling2;
    field1IsAinferSibling2();
    field1 = bottom;
    field1IsAinferSibling2();

    parent = sibling1;
    parentIsAinferSibling1();
  }
}
