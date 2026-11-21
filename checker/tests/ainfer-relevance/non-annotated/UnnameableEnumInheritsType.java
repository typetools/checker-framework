// Every enum inherits the member type `EnumDesc` from its implicit superclass `java.lang.Enum`,
// and that inherited member type shadows the type of the same name that is declared in the same
// package.  An enum that is declared in a local class has no name that inference can look up, so
// inference determines its member types from its supertypes, which must therefore include the
// implicit `java.lang.Enum`.  Inference must resolve the name "EnumDesc" to
// `java.lang.Enum.EnumDesc`, which is irrelevant, rather than to the top-level class `EnumDesc`,
// which is relevant.  The goal file shows that inference writes no annotation for
// `enumDescField`, but does write one for `charSequenceField`.
public class UnnameableEnumInheritsType {

  static void declareLocalClass() {
    class Local {
      enum Nested {
        CONSTANT;

        EnumDesc enumDescField;

        CharSequence charSequenceField;

        void assignFields(EnumDesc e, CharSequence c) {
          enumDescField = e;
          charSequenceField = c;
        }
      }
    }
  }
}
