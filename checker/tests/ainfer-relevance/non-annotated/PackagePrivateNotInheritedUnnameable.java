import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// Like `PackagePrivateNotInherited.java`, but the subclass of `otherpackage.OtherPackageSuperclass`
// is an anonymous class and a local class, neither of which has a name that inference can look up.
// Inference must search the supertype, but it must not conclude that the unnameable class inherits
// the supertype's package-private member type `otherpackage.OtherPackageSuperclass.List`, because
// this compilation unit is in the unnamed package.  Nor does the unnameable class inherit
// `otherpackage.OtherPackageGrandparent.List`, which `OtherPackageSuperclass` hides.  Inference
// must therefore resolve the name "List" to the top-level class `List` in the unnamed package,
// which is relevant because it is a subtype of `CharSequence`; both member classes named `List`
// are irrelevant.  If inference resolves the name incorrectly and therefore discards the
// annotation, then the second (validation) pass of this test issues the warnings that are written
// below.
public class PackagePrivateNotInheritedUnnameable {

  static Object anonymous =
      new otherpackage.OtherPackageSuperclass() {

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
      };

  static void declareLocalClass() {
    class Local extends otherpackage.OtherPackageSuperclass {

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
  }
}
