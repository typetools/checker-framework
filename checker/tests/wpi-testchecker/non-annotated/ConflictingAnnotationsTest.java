// Tests whether inferring an @Sibling1 annotation when another @Sibling1 annotation in the default
// package is present causes problems. Conflicting annotations that are not in the default package
// are not a problem, because TypeMirror#toString prints their fully-qualified names, making
// namespace collisions impossible.

public class ConflictingAnnotationsTest {

  int getWPINamespaceSibling1() {
    return getSibling1();
  }

  // This version of Sibling1 is not typechecked - it doesn't belong to the checker and instead is
  // defined in the Sibling1.java file in this directory.
  @Sibling1 Object getLocalSibling1(Object o) {
    return o;
  }

  void test() {
    // :: warning: argument.type.incompatible
    expectsSibling1(getWPINamespaceSibling1());
  }

  @org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1 int getSibling1() {
    return 1;
  }

  void expectsSibling1(
      @org.checkerframework.checker.testchecker.wholeprograminference.qual.Sibling1 int i) {}
}
