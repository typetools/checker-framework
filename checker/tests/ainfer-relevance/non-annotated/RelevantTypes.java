import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// AinferRelevanceTestChecker declares `@RelevantJavaTypes({CharSequence.class, int.class})`.
// Whole-program inference must write, into the .ajava file, every annotation that it infers for a
// relevant type:  `int`, which is listed; and `String` and `CharSequence`, which are relevant
// because of subtyping.  If inference discards such an annotation, then the second (validation)
// pass of this test issues the warnings that are written below.
public class RelevantTypes {

  static int intField;
  static String stringField;
  static CharSequence charSequenceField;

  static void assignFields() {
    intField = getSibling1Int();
    stringField = getSibling1String();
    charSequenceField = getSibling1String();
  }

  static void useFields() {
    // :: warning: [argument]
    expectsSibling1Int(intField);
    // :: warning: [argument]
    expectsSibling1String(stringField);
    // :: warning: [argument]
    expectsSibling1CharSequence(charSequenceField);
  }

  static int intReturn() {
    return getSibling1Int();
  }

  static String stringReturn() {
    return getSibling1String();
  }

  static void useReturns() {
    // :: warning: [argument]
    expectsSibling1Int(intReturn());
    // :: warning: [argument]
    expectsSibling1String(stringReturn());
  }

  // A leading annotation on a varargs formal parameter is on the parameter's element type, not on
  // the array type that `...` creates.
  static void varargs(String... args) {}

  static void callVarargs() {
    varargs(getSibling1String());
  }

  static void expectsSibling1Int(@AinferSibling1 int i) {}

  static void expectsSibling1String(@AinferSibling1 String s) {}

  static void expectsSibling1CharSequence(@AinferSibling1 CharSequence cs) {}

  static @AinferSibling1 int getSibling1Int() {
    return (@AinferSibling1 int) 0;
  }

  @SuppressWarnings("cast.unsafe") // This is how to obtain a @AinferSibling1 String.
  static @AinferSibling1 String getSibling1String() {
    return (@AinferSibling1 String) "";
  }
}
