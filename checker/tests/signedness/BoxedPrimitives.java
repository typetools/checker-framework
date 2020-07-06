import java.util.LinkedList;
import org.checkerframework.checker.signedness.qual.Signed;
import org.checkerframework.checker.signedness.qual.Unsigned;

public class BoxedPrimitives {

    @Signed int si;
    @Unsigned int ui;

    @Signed Integer sbi;
    @Unsigned Integer ubi;

    void argSigned(@Signed int x) {
        si = x;
        sbi = x;
        // :: error: (assignment.type.incompatible)
        ui = x;
        // :: error: (assignment.type.incompatible)
        ubi = x;
    }

    void argUnsigned(@Unsigned int x) {
        // :: error: (assignment.type.incompatible)
        si = x;
        // :: error: (assignment.type.incompatible)
        sbi = x;
        ui = x;
        ubi = x;
    }

    void argSignedBoxed(@Signed Integer x) {
        si = x;
        sbi = x;
        // :: error: (assignment.type.incompatible)
        ui = x;
        // :: error: (assignment.type.incompatible)
        ubi = x;
    }

    void argUnsignedBoxed(@Unsigned Integer x) {
        // :: error: (assignment.type.incompatible)
        si = x;
        // :: error: (assignment.type.incompatible)
        sbi = x;
        ui = x;
        ubi = x;
    }

    void client() {
        argSigned(si);
        argSigned(sbi);
        // :: error: (argument.type.incompatible)
        argUnsigned(si);
        // :: error: (argument.type.incompatible)
        argUnsigned(sbi);
        // :: error: (argument.type.incompatible)
        argSigned(ui);
        // :: error: (argument.type.incompatible)
        argSigned(ubi);
        argUnsigned(ui);
        argUnsigned(ubi);
    }

    public LinkedList<Integer> commands;

    void forLoop() {
        for (Integer ix : this.commands) {
            argSigned(ix);
        }
    }
}
