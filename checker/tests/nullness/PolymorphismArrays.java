import org.checkerframework.checker.nullness.qual.*;

public class PolymorphismArrays {

  public PolymorphismArrays(String[][] elts) {
    this.elts = elts;
  }

  public static boolean @PolyNull [] bad(boolean @PolyNull [] seq) {
    // Cannot directly return null;
    // :: error: (return)
    return null;
  }

  public static boolean @PolyNull [] slice(boolean @PolyNull [] seq, int start, int end) {
    // Know from comparison that argument is nullable -> also return is nullable.
    if (seq == null) {
      return null;
    }
    return new boolean[] {};
  }

  public static boolean @PolyNull [] slice(boolean @PolyNull [] seq, long start, int end) {
    return slice(seq, (int) start, end);
  }

  public static @PolyNull String[] intern(@PolyNull String[] a) {
    return a;
  }

  // from OneOfStringSequence.java
  private String[][] elts;

  @SuppressWarnings("purity") // ignore, analysis too strict.
  @org.checkerframework.dataflow.qual.Pure
  public PolymorphismArrays clone() {
    PolymorphismArrays result = new PolymorphismArrays(elts.clone());
    for (int i = 0; i < elts.length; i++) {
      result.elts[i] = intern(elts[i].clone());
    }
    return result;
  }

  public void simplified() {
    String[][] elts = new String[0][0];
    String[][] clone = elts.clone();
    String[] results = intern(elts[0].clone());
  }

  public static <T> int indexOf(T[] a) {
    return indexOfEq(a);
  }

  public static int indexOfEq(@PolyNull Object[] a) {
    return -1;
  }
}
