// Inference must not write, into the .ajava file, an annotation that it infers for an irrelevant
// type:  `double`, which is not listed in the checker's `@RelevantJavaTypes` and is not related to
// a listed type by subtyping; or an array, because `Object[].class` is not listed.  Because the
// checker treats these types as irrelevant, omitting the annotations does not change the result of
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
