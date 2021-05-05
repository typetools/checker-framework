import org.checkerframework.checker.testchecker.wholeprograminference.qual.Parent;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling2;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Top;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.WholeProgramInferenceBottom;

class EnsuresQualifierTest {

  @Top int field1;
  @Top int field2;

  @Top int top;
  @Parent int parent;
  @Sibling1 int sibling1;
  @Sibling2 int sibling2;
  @WholeProgramInferenceBottom int bottom;

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

  void field1IsSibling2() {
    field1 = sibling2;
  }

  void field1IsSibling2_2(boolean b) {
    if (b) {
      field1 = sibling2;
    } else {
      field1 = bottom;
    }
  }

  void parentIsSibling1() {
    parent = sibling1;
  }

  // Prevent refinement of the `parent` field variable.
  void parentIsParent(@Parent int x) {
    parent = x;
  }

  void noEnsures() {}

  void client1() {
    field1IsParent();
    // :: warning: (assignment)
    @Parent int p = field1;
  }

  void client2() {
    field1IsParent_2(true);
    // :: warning: (assignment)
    @Parent int p = field1;
  }

  void client3() {
    field1IsSibling2();
    // :: warning: (assignment)
    @Sibling2 int x = field1;
  }

  void client4() {
    field1IsSibling2_2(true);
    // :: warning: (assignment)
    @Sibling2 int x = field1;
  }

  void client5() {
    parentIsSibling1();
    // :: warning: (assignment)
    @Sibling1 int x = parent;
  }
}
