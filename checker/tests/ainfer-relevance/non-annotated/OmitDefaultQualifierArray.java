import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// `@AinferDefaultType` is the default qualifier of AinferRelevanceTestChecker.  Whole-program
// inference infers `@AinferDefaultType` for every array component type `CharSequence` in these
// locations:  the types of `field` and `field2D`, the formal parameters of `setField`,
// `setField2D`, and `setFieldVarargs`, and the return type of `getField`.  Writing
// `@AinferDefaultType` there would be redundant, so the goal file omits it.  By contrast,
// inference infers `@AinferSibling1` for the component type of `sibling1Field`, and the goal file
// contains it.  (An array type is irrelevant to the checker, so no annotation is written on the
// array type itself.)
public class OmitDefaultQualifierArray {

  CharSequence[] field;

  CharSequence[][] field2D;

  CharSequence[] sibling1Field;

  void setField(CharSequence[] cs) {
    field = cs;
  }

  void setField2D(CharSequence[][] cs) {
    field2D = cs;
  }

  void setFieldVarargs(CharSequence... cs) {
    field = cs;
  }

  void setSibling1Field(@AinferSibling1 CharSequence[] cs) {
    sibling1Field = cs;
  }

  CharSequence[] getField() {
    return field;
  }

  static void client(CharSequence[] cs, CharSequence[][] cs2D, CharSequence c) {
    OmitDefaultQualifierArray o = new OmitDefaultQualifierArray();
    o.setField(cs);
    o.setField2D(cs2D);
    o.setFieldVarargs(c, c);
    o.getField();
  }
}
