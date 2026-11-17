import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// Every enum inherits the member type `EnumDesc` from its implicit superclass `java.lang.Enum`,
// and that inherited member type shadows the type of the same name that is declared in the same
// package.  An enum that is declared in a local class has no name that inference can look up, so
// inference determines its member types from its supertypes, which must therefore include the
// implicit `java.lang.Enum`.  Inference must resolve the name "EnumDesc" to
// `java.lang.Enum.EnumDesc`, which is irrelevant, rather than to the top-level class `EnumDesc`,
// which is relevant.  Inference infers `@AinferSibling1` for both fields.  The goal file shows
// that inference discards the annotation on `enumDescField`, but writes the annotation on
// `charSequenceField`.
//
// Writing `@AinferSibling1` on a `java.lang.Enum.EnumDesc` is itself a relevance violation, which
// this test suppresses because the warning would appear in both passes of the test and the
// annotated copy of this file contains no expected diagnostics.
public class UnnameableEnumInheritsType {

  static void declareLocalClass() {
    class Local {
      enum Nested {
        CONSTANT;

        EnumDesc enumDescField;

        CharSequence charSequenceField;

        @SuppressWarnings("anno.on.irrelevant")
        void assignFields(@AinferSibling1 EnumDesc e, @AinferSibling1 CharSequence c) {
          enumDescField = e;
          charSequenceField = c;
        }
      }
    }
  }
}
