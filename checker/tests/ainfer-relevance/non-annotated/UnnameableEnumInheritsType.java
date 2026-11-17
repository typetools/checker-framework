import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// Every enum inherits the member type `EnumDesc` from its implicit superclass `java.lang.Enum`,
// and that inherited member type shadows the type of the same name that is declared in the same
// package.  An enum that is declared in a local class has no name that inference can look up, so
// inference determines its member types from its supertypes, which must therefore include the
// implicit `java.lang.Enum`.  Inference must resolve the name "EnumDesc" to
// `java.lang.Enum.EnumDesc`, which is irrelevant, rather than to the top-level class `EnumDesc`,
// which is relevant.  The goal file shows that inference writes no annotation for
// `enumDescField`, but does write `@AinferSibling1` for `charSequenceField`.  If inference
// discards that annotation, then the second (validation) pass of this test issues the warning
// that is written below.
public class UnnameableEnumInheritsType {

  static void declareLocalClass() {
    class Local {
      enum Nested {
        CONSTANT;

        EnumDesc enumDescField;

        CharSequence charSequenceField;

        void assignFields(EnumDesc e) {
          enumDescField = e;
          charSequenceField = getSibling1();
        }

        void useField() {
          // :: warning: [argument]
          expectsSibling1(charSequenceField);
        }

        void expectsSibling1(@AinferSibling1 CharSequence c) {}

        @AinferSibling1 CharSequence getSibling1() {
          return null;
        }
      }
    }
  }
}
