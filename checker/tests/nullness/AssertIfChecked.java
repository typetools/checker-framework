import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.checker.nullness.qual.EnsuresNonNullIf;

public class AssertIfChecked {

    boolean unknown = false;

    @Nullable Object value;

    @EnsuresNonNullIf(result = true, expression = "value")
    // :: error: (contracts.conditional.postcondition.invalid.returntype)
    public void badform1() {}

    @EnsuresNonNullIf(result = true, expression = "value")
    // :: error: (contracts.conditional.postcondition.invalid.returntype)
    public Object badform2() {
        return new Object();
    }

    @EnsuresNonNullIf(result = false, expression = "value")
    // :: error: (contracts.conditional.postcondition.invalid.returntype)
    public void badform3() {}

    @EnsuresNonNullIf(result = false, expression = "value")
    // :: error: (contracts.conditional.postcondition.invalid.returntype)
    public Object badform4() {
        return new Object();
    }

    @EnsuresNonNullIf(result = true, expression = "value")
    public boolean goodt1() {
        return value != null;
    }

    @EnsuresNonNullIf(result = true, expression = "value")
    public boolean badt1() {
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return value == null;
    }

    @EnsuresNonNullIf(result = false, expression = "value")
    public boolean goodf1() {
        return value == null;
    }

    @EnsuresNonNullIf(result = false, expression = "value")
    public boolean badf1() {
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return value != null;
    }

    @EnsuresNonNullIf(result = true, expression = "value")
    public boolean bad2() {
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return value == null || unknown;
    }

    @EnsuresNonNullIf(result = false, expression = "value")
    public boolean bad3() {
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return value == null && unknown;
    }

    @EnsuresNonNullIf(result = true, expression = "#1")
    boolean testParam(final @Nullable Object param) {
        return param != null;
    }

    @EnsuresNonNullIf(result = true, expression = "#1")
    boolean testLitTTgood1(final @Nullable Object param) {
        if (param == null) return false;
        return true;
    }

    @EnsuresNonNullIf(result = true, expression = "#1")
    boolean testLitTTbad1(final @Nullable Object param) {
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return true;
    }

    @EnsuresNonNullIf(result = false, expression = "#1")
    boolean testLitFFgood1(final @Nullable Object param) {
        return true;
    }

    @EnsuresNonNullIf(result = false, expression = "#1")
    boolean testLitFFgood2(final @Nullable Object param) {
        if (param == null) return true;
        return false;
    }

    @EnsuresNonNullIf(result = false, expression = "#1")
    boolean testLitFFbad1(final @Nullable Object param) {
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        if (param == null) return false;
        return true;
    }

    @EnsuresNonNullIf(result = false, expression = "#1")
    boolean testLitFFbad2(final @Nullable Object param) {
        // :: error: (contracts.conditional.postcondition.not.satisfied)
        return false;
    }

    @Nullable Object getValueUnpure() {
        return value;
    }

    @org.checkerframework.dataflow.qual.Pure
    @Nullable Object getValuePure() {
        return value;
    }

    @EnsuresNonNullIf(result = true, expression = "getValuePure()")
    public boolean hasValuePure() {
        return getValuePure() != null;
    }

    @EnsuresNonNullIf(result = true, expression = "#1")
    public static final boolean isComment(@Nullable String s) {
        return s != null && (s.startsWith("//") || s.startsWith("#"));
    }

    @EnsuresNonNullIf(result = true, expression = "#1")
    public boolean myEquals(@Nullable Object o) {
        return (o instanceof String) && equals((String) o);
    }

    /*
     * The next two methods are from Daikon's class Quant. They verify that
     * EnsuresNonNullIf is correctly added to the assumptions after a check.
     */

    @EnsuresNonNullIf(
            result = true,
            expression = {"#1", "#2"})
    /* pure */ public static boolean sameLength(
            boolean @Nullable [] seq1, boolean @Nullable [] seq2) {
        return ((seq1 != null) && (seq2 != null) && seq1.length == seq2.length);
    }

    /* pure */ public static boolean isReverse(
            boolean @Nullable [] seq1, boolean @Nullable [] seq2) {
        if (!sameLength(seq1, seq2)) {
            return false;
        }
        // This assert is not needed for inference.
        // assert seq1 != null && seq2 != null; // because sameLength() = true

        int length = seq1.length;
        for (int i = 0; i < length; i++) {
            if (seq1[i] != seq2[length - i - 1]) {
                return false;
            }
        }
        return true;
    }
}
