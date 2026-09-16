// Inference infers annotations for types that this checker treats as irrelevant:  `double`, which
// is not listed in the checker's `@RelevantJavaTypes` and is not related to a listed type by
// subtyping; and arrays, because `Object[].class` is not listed.  The goal file records that
// inference currently writes those annotations into the .ajava file.  They are clutter:  because
// the checker treats these types as irrelevant, omitting them would not change the result of
// type-checking.
public class IrrelevantTypes {

  static double doubleField;
  static String[] arrayField;
  static java.util.Date dateField;

  static void assignFields() {
    doubleField = 0.0;
    arrayField = new String[] {""};
    dateField = new java.util.Date();
  }

  static double doubleReturn() {
    return doubleField;
  }

  static String[] arrayReturn() {
    return arrayField;
  }
}
