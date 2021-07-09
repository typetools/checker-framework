import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.Parent;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling2;
import org.checkerframework.checker.testchecker.ainfer.qual.Top;

class EnsuresQualifierParamsTest {

  @Top int field1;
  @Top int field2;

  @Top int top;
  @Parent int parent;
  @Sibling1 int sibling1;
  @Sibling2 int sibling2;
  @AinferBottom int bottom;

  void argIsParent(int arg) {
    arg = parent;
  }

  void argIsParent_2(int arg, boolean b) {
    if (b) {
      arg = sibling1;
    } else {
      arg = sibling2;
    }
  }

  void argIsSibling2(int arg) {
    arg = sibling2;
  }

  void argIsSibling2_2(int arg, boolean b) {
    if (b) {
      arg = sibling2;
    } else {
      arg = bottom;
    }
  }

  void noEnsures() {}

  void client1(int arg) {
    argIsParent(arg);
    // :: warning: (assignment)
    @Parent int p = arg;
  }

  void client2(int arg) {
    argIsParent_2(arg, true);
    // :: warning: (assignment)
    @Parent int p = arg;
  }

  void client3(int arg) {
    argIsSibling2(arg);
    // :: warning: (assignment)
    @Sibling2 int x = arg;
  }

  void client4(int arg) {
    argIsSibling2_2(arg, true);
    // :: warning: (assignment)
    @Sibling2 int x = arg;
  }
}
