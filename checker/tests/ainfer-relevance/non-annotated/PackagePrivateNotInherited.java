import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// A class does not inherit a package-private member type of a superclass that is in a different
// package, and a declaration hides what its declaring class would otherwise inherit even when the
// declaration itself is not inherited.  This class therefore inherits neither
// `otherpackage.OtherPackageSuperclass.List` (which is package-private, and this compilation unit
// is in the unnamed package) nor `otherpackage.OtherPackageGrandparent.List` (which is public, but
// which `OtherPackageSuperclass` does not inherit, because `OtherPackageSuperclass` declares a
// member type of the same name).  Inference must resolve the name "List" to the top-level class
// `List` in the unnamed package, which is relevant because it is a subtype of `CharSequence`;
// both member classes named `List` are irrelevant.  If inference resolves the name incorrectly and
// therefore discards the annotation, then the second (validation) pass of this test issues the
// warning that is written below.
public class PackagePrivateNotInherited extends otherpackage.OtherPackageSuperclass {

  List field;

  void assignField() {
    field = getSibling1();
  }

  void useField() {
    // :: warning: [argument]
    expectsSibling1(field);
  }

  void expectsSibling1(@AinferSibling1 List l) {}

  @AinferSibling1 List getSibling1() {
    return null;
  }
}
