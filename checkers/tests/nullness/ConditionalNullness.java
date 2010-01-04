import checkers.nullness.quals.*;
import java.util.*;
public class ConditionalNullness {

    @AssertNonNullIfTrue({"field", "method()"})
    boolean checkNonNull() { return false; }

    @Nullable Object field = null;
    @Nullable Object method() { return "m"; }

    void testSelfWithCheck() {
        ConditionalNullness other = new ConditionalNullness();
        if (checkNonNull()) {
            field.toString();
            method().toString();
            other.field.toString(); // error
            other.method().toString();  // error
        }
        method().toString();   // error
    }

    void testSelfWithoutCheck() {
        field.toString();       // error
        method().toString();    // error
    }

    void testSelfWithCheckNegation() {
        if (checkNonNull()) { }
        else {
            field.toString();   // error
        }
        field.toString();       // error
    }

    void testOtherWithCheck() {
        ConditionalNullness other = new ConditionalNullness();
        if (other.checkNonNull()) {
            other.field.toString();
            other.method().toString();
            field.toString();   // error
            method().toString(); // error
        }
        other.method().toString();  // error
        method().toString();   // error
    }

    void testOtherWithoutCheck() {
        ConditionalNullness other = new ConditionalNullness();
        other.field.toString();     // error
        other.method().toString();  // error
        field.toString();       // error
        method().toString();    // error
    }

    void testOtherWithCheckNegation() {
        ConditionalNullness other = new ConditionalNullness();
        if (other.checkNonNull()) { }
        else {
            other.field.toString();     // error
            other.method().toString();  // error
            field.toString();   // error
        }
        field.toString();       // error
    }

    /** True iff both sequences are non-null and have the same length. */
    @AssertNonNullIfTrue({"seq1", "seq2"})
    /* pure */ public static boolean sameLength(boolean @Nullable [] seq1, boolean @Nullable [] seq2) {
        return ((seq1 != null)
                && (seq2 != null)
                && seq1.length == seq2.length);
    }

    /* pure */ public static boolean pairwiseEqual(boolean @Nullable [] seq3, boolean @Nullable [] seq4) {
        if (! sameLength(seq3, seq4)) { return false; }
        boolean b1 = seq3[0];
        boolean b2 = seq4[0];
        return true;
    }

}
