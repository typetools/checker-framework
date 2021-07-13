import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.Parent;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.Sibling2;

import org.checkerframework.framework.qual.EnsuresQualifier;

class EnsuresQualifierParamsTest {

  // these methods are used to infer types

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = Parent.class)
  void becomeParent(int arg) { }

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = Sibling1.class)
  void becomeSibling1(int arg) { }

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = Sibling2.class)
  void becomeSibling2(int arg) { }

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = AinferBottom.class)
  void becomeBottom(int arg) { }

  // these methods should have types inferred for them

  void argIsParent(int arg) {
    becomeParent(arg);
  }

  void argIsParent_2(int arg, boolean b) {
    if (b) {
      becomeSibling1(arg);
    } else {
      becomeSibling2(arg);
    }
  }

  void argIsSibling2(int arg) {
    becomeSibling2(arg);
  }

  void argIsSibling2_2(int arg, boolean b) {
    if (b) {
      becomeSibling2(arg);
    } else {
      becomeBottom(arg);
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
