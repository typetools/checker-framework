import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class WhileTest {
    @Nullable Integer z;
    @NonNull Integer nnz = new Integer(22);

    public static void main(String[] args) {
        new WhileTest().testwhile1();
    }

    public void testwhile1() {
        z = null;
        // :: error: (assignment.type.incompatible)
        nnz = z;

        while (z == null) {
            break;
        }
        // :: error: (assignment.type.incompatible)
        nnz = z;
        nnz.toString();
    }

    public void testwhile2() {
        z = null;
        while (z == null) {;
        }
        nnz = z;
    }

    public void testdo1() {
        z = null;
        do {
            break;
        } while (z == null);
        // :: error: (assignment.type.incompatible)
        nnz = z;
    }

    public void testdo2() {
        z = null;
        do {;
        } while (z == null);
        nnz = z;
    }

    public void testfor1() {
        z = null;
        for (; z == null; ) {
            break;
        }
        // :: error: (assignment.type.incompatible)
        nnz = z;
    }

    public void testfor2() {
        z = null;
        for (; z == null; ) {;
        }
        nnz = z;
    }
}
