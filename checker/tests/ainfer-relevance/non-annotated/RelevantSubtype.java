import org.checkerframework.checker.testchecker.ainfer.qual.AinferSibling1;

// This class is relevant because it is a subtype of CharSequence, which the checker lists in its
// `@RelevantJavaTypes`.  Relevance is a question about subtyping, not about names:  the name
// "RelevantSubtype" does not appear in `@RelevantJavaTypes`.
public class RelevantSubtype implements CharSequence {

  @Override
  public int length() {
    return 0;
  }

  @Override
  public char charAt(int index) {
    return 'a';
  }

  @Override
  public CharSequence subSequence(int start, int end) {
    return this;
  }

  static RelevantSubtype field;

  static void assignField() {
    field = getSibling1();
  }

  static void useField() {
    // :: warning: [argument]
    expectsSibling1(field);
  }

  static void expectsSibling1(@AinferSibling1 RelevantSubtype r) {}

  @SuppressWarnings("cast.unsafe") // This is how to obtain a @AinferSibling1 RelevantSubtype.
  static @AinferSibling1 RelevantSubtype getSibling1() {
    return (@AinferSibling1 RelevantSubtype) new RelevantSubtype();
  }
}
