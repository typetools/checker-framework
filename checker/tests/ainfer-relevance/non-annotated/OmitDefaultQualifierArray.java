// `@AinferDefaultType` is the default qualifier of AinferRelevanceTestChecker.  Whole-program
// inference infers `@AinferDefaultType` for the component type `CharSequence` of each array type
// below:  the type of the field, and the types of the formal parameter and return type of each
// method.  Writing `@AinferDefaultType` there would be redundant, so the goal file omits it.  (An
// array type is irrelevant to the checker, so no annotation is written on the array type itself.)
public class OmitDefaultQualifierArray {

  CharSequence[] field;

  void setField(CharSequence[] cs) {
    field = cs;
  }

  CharSequence[] getField() {
    return field;
  }

  static void client(CharSequence[] cs) {
    OmitDefaultQualifierArray o = new OmitDefaultQualifierArray();
    o.setField(cs);
    o.getField();
  }
}
