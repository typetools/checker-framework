import org.checkerframework.checker.testchecker.ainfer.qual.AinferBottom;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferParent;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;
import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling2;
import org.checkerframework.framework.qual.EnsuresQualifier;

class EnsuresQualifierParamsTest {

  // these methods are used to infer types

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = AinferParent.class)
  void becomeParent(Object arg) {}

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = AinferSibling1.class)
  void becomeAinferSibling1(Object arg) {}

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = AinferSibling2.class)
  void becomeAinferSibling2(Object arg) {}

  @SuppressWarnings("contracts.postcondition") // establish ground truth
  @EnsuresQualifier(expression = "#1", qualifier = AinferBottom.class)
  void becomeBottom(Object arg) {}

  // these methods should have types inferred for them

  void argIsParent(Object arg) {
    becomeParent(arg);
  }

  void argIsParent_2(Object arg, boolean b) {
    if (b) {
      becomeAinferSibling1(arg);
    } else {
      becomeAinferSibling2(arg);
    }
  }

  void argIsAinferSibling2(Object arg) {
    becomeAinferSibling2(arg);
  }

  void argIsAinferSibling2_2(Object arg, boolean b) {
    if (b) {
      becomeAinferSibling2(arg);
    } else {
      becomeBottom(arg);
    }
  }

  void thisIsParent() {
    becomeParent(this);
  }

  void thisIsParent_2(boolean b) {
    if (b) {
      becomeAinferSibling1(this);
    } else {
      becomeAinferSibling2(this);
    }
  }

  void thisIsParent_2_2(boolean b) {
    if (b) {
      becomeAinferSibling2(this);
    } else {
      becomeAinferSibling1(this);
    }
  }

  void thisIsParent_3(boolean b) {
    if (b) {
      becomeAinferSibling1(this);
    } else {
      becomeAinferSibling2(this);
    }
    noEnsures();
  }

  void thisIsEmpty(boolean b) {
    if (b) {
      // do nothing
      this.noEnsures();
    } else {
      becomeAinferSibling1(this);
    }
  }

  void thisIsAinferSibling2() {
    becomeAinferSibling2(this);
  }

  void thisIsAinferSibling2_2(boolean b) {
    if (b) {
      becomeAinferSibling2(this);
    } else {
      becomeBottom(this);
    }
  }

  void thisIsAinferSibling2_2_2(boolean b) {
    if (b) {
      becomeBottom(this);
    } else {
      becomeAinferSibling2(this);
    }
  }

  void noEnsures() {}

  void client1(Object arg) {
    argIsParent(arg);
    // :: warning: (assignment)
    @AinferParent Object p = arg;
  }

  void client2(Object arg) {
    argIsParent_2(arg, true);
    // :: warning: (assignment)
    @AinferParent Object p = arg;
  }

  void client3(Object arg) {
    argIsAinferSibling2(arg);
    // :: warning: (assignment)
    @AinferSibling2 Object x = arg;
  }

  void client4(Object arg) {
    argIsAinferSibling2_2(arg, true);
    // :: warning: (assignment)
    @AinferSibling2 Object x = arg;
  }

  void clientThis1() {
    thisIsParent();
    // :: warning: (assignment)
    @AinferParent Object o = this;
  }

  void clientThis2() {
    thisIsParent_2(true);
    // :: warning: (assignment)
    @AinferParent Object o = this;
  }

  void clientThis2_2() {
    thisIsParent_2(false);
    // :: warning: (assignment)
    @AinferParent Object o = this;
  }

  void clientThis2_3() {
    thisIsParent_3(false);
    // :: warning: (assignment)
    @AinferParent Object o = this;
  }

  void clientThis3() {
    thisIsAinferSibling2();
    // :: warning: (assignment)
    @AinferSibling2 Object o = this;
  }

  void clientThis4() {
    thisIsAinferSibling2_2(true);
    // :: warning: (assignment)
    @AinferSibling2 Object o = this;
  }

  void clientThis5() {
    thisIsAinferSibling2_2_2(true);
    // :: warning: (assignment)
    @AinferSibling2 Object o = this;
  }

  void clientThis6() {
    thisIsParent_2_2(true);
    // :: warning: (assignment)
    @AinferParent Object o = this;
  }
}
