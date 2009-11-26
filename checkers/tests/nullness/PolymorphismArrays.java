import checkers.nullness.quals.*;

public class PolymorphismArrays {

    public static boolean @PolyNull [] slice(boolean @PolyNull [] seq, int start, int end) {
        if (seq == null) { return null; }
        return new boolean[] { };
    }

    public static boolean @PolyNull [] slice(boolean @PolyNull [] seq, long start, int end) {
        return slice(seq, (int)start, end);
    }

    public static @PolyNull String [] intern(@PolyNull String[] a) {
        return a;
    }

    // from OneOfStringSequence.java
    private String[][] elts;
    public PolymorphismArrays clone() {
        PolymorphismArrays result = new PolymorphismArrays();
        result.elts = elts.clone();
        for (int i=0; i < elts.length; i++) {
            result.elts[i] = intern(elts[i].clone());
        }
        return result;
    }

    public void simplified() {
        String[][] elts = new String[0][0];
        String[][] clone = elts.clone();
        String[] results = intern(elts[0].clone());
    }

}
