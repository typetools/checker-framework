import org.checkerframework.checker.testchecker.wholeprograminference.qual.Parent;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling2;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.Top;
import org.checkerframework.checker.testchecker.wholeprograminference.qual.WholeProgramInferenceBottom;

class RequiresQualifierTest {

  @Top int field1;
  @Top int field2;

  @Top int top;
  @Parent int parent;
  @Sibling1 int sibling1;
  @Sibling2 int sibling2;
  @WholeProgramInferenceBottom int bottom;

  void field1IsParent() {
    // :: warning: (assignment.type.incompatible)
    @Parent int x = field1;
  }

  void field1IsSibling2() {
    // :: warning: (assignment.type.incompatible)
    @Sibling2 int x = field1;
  }

  void parentIsSibling1() {
    // :: warning: (assignment.type.incompatible)
    @Sibling1 int x = parent;
  }

  void noRequirements() {}

  void client2(@Parent int p) {
    field1 = p;
    field1IsParent();
  }

  void client1() {
    noRequirements();

    field1 = parent;
    field1IsParent();

    field1 = sibling2;
    field1IsSibling2();
    field1 = bottom;
    field1IsSibling2();

    parent = sibling1;
    parentIsSibling1();
  }
}
