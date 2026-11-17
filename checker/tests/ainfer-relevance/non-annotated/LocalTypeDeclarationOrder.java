import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// The scope of a local class declaration is the rest of the block that contains it, including the
// declaration itself.  A use of the same name that appears earlier in the block is not in that
// scope; it refers to some other type.  The name "Foo" in `UseBefore` refers to the member class
// `LocalTypeDeclarationOrder.Foo`, which is irrelevant because it is not a subtype of
// `CharSequence`, so inference must not write an annotation on it.  The name "Foo" in `UseAfter`
// refers to the local class, which `Elements` cannot look up by name, so inference conservatively
// writes the `@AinferSibling1` annotation that it infers.  If inference instead resolves that name
// to the irrelevant member class and therefore discards the annotation, then the second
// (validation) pass of this test issues the warning that is written below.
public class LocalTypeDeclarationOrder {

  static class Foo {}

  static void declarationOrder() {

    class UseBefore {
      Foo field;

      void assignField() {
        field = new Foo();
      }
    }

    class Foo implements CharSequence {

      @Override
      public int length() {
        return 0;
      }

      @Override
      public char charAt(int index) {
        return ' ';
      }

      @Override
      public CharSequence subSequence(int start, int end) {
        return this;
      }
    }

    class UseAfter {
      Foo field;

      void assignField() {
        field = getSibling1();
      }

      void useField() {
        // :: warning: [argument]
        expectsSibling1(field);
      }

      void expectsSibling1(@AinferSibling1 Foo f) {}

      @AinferSibling1 Foo getSibling1() {
        return null;
      }
    }
  }
}
