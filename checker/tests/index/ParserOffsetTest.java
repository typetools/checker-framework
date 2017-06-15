import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

public class ParserOffsetTest {

    public void subtraction1(String[] a, @IndexFor("#1") int i) {
        int length = a.length;
        if (i >= length - 1 || a[i + 1] == null) {
            // body is irrelevant
        }
    }

    public void addition1(String[] a, @IndexFor("#1") int i) {
        int length = a.length;
        if ((i + 1) >= length || a[i + 1] == null) {
            // body is irrelevant
        }
    }

    public @IndexFor("#1") int subtraction2(String[] a, @IndexFor("#1") int i) {
        if (i < a.length - 1) {
            return i + 1;
        } else {
            return i;
        }
    }

    public @IndexFor("#1") int addition2(String[] a, @IndexFor("#1") int i) {
        if ((i + 1) < a.length) {
            return i + 1;
        } else {
            return i;
        }
    }

    public @IndexFor("#1") int addition3(String[] a, @IndexFor("#1") int i) {
        if ((i + 5) < a.length) {
            return i + 5;
        } else {
            return i;
        }
    }

    @SuppressWarnings("lowerbound")
    public @LTLengthOf("#1") int subtraction3(String[] a, @NonNegative int k) {
        if (k - 5 < a.length) {
            String s = a[k - 5];
            return k - 5;
        } else {
            return a.length - 1;
        }
    }

    @SuppressWarnings({"lowerbound", "local.variable.unsafe.dependent.annotation"})
    public void subtraction4(String[] a, @IndexFor("#1") int i) {
        if (1 - i < a.length) {
            // The error on this assignment is a false positive.
            //:: error: (assignment.type.incompatible)
            @IndexFor("a") int j = 1 - i;

            //:: error: (assignment.type.incompatible)
            @LTLengthOf(value = "a", offset = "1") int k = i;
        }
    }

    public @LTLengthOf("#1") int subtraction5(String[] a, int i) {
        if (1 - i < a.length) {
            //:: error: (return.type.incompatible)
            return i;
        } else {
            return a.length - 1;
        }
    }

    public @LTLengthOf("#1") int subtraction6(String[] a, int i, int j) {
        if (i - j < a.length - 1) {
            return i - j;
        } else {
            return a.length - 1;
        }
    }

    public @LTLengthOf("#1") int subtraction7(String[] a, int i, int j) {
        if (i - j < a.length - 1) {
            //:: error: (return.type.incompatible)
            return i;
        } else {
            return a.length - 1;
        }
    }

    public @IndexFor("#1") Integer multiplication1(String[] a, int i, @Positive int j) {
        if ((i * j) < (a.length + j)) {
            //:: error: (return.type.incompatible)
            return i;
        } else {
            return null;
        }
    }

    public @IndexFor("#1") Integer multiplication1again(String[] a, int i, @Positive int j) {
        if ((i * j) < (a.length + j)) {
            //:: error: (return.type.incompatible)
            return j;
        } else {
            return null;
        }
    }

    @SuppressWarnings("local.variable.unsafe.dependent.annotation")
    public void multiplication2(String @ArrayLen(5) [] a, @IntVal(-2) int i, @IntVal(20) int j) {
        if ((i * j) < (a.length - 20)) {
            @LTLengthOf("a") int k1 = i;
            //:: error: (assignment.type.incompatible)
            @LTLengthOf(value = "a", offset = "20") int k2 = i;
            //:: error: (assignment.type.incompatible)
            @LTLengthOf("a") int k3 = j;
        }
    }
}
