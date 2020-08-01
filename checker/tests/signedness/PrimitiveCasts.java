import org.checkerframework.checker.signedness.qual.Unsigned;

public class PrimitiveCasts {

    void shortToChar1(short s) {
        // :: warning: (cast.unsafe)
        char c = (char) s;
    }

    // These are Java errors.
    // void shortToChar2(short s) {
    //     char c = s;
    // }
    // char shortToChar3(short s) {
    //     return s;
    // }

    void intToDouble1(@Unsigned int ui) {
        // :: warning: (cast.unsafe)
        double d = (double) ui;
    }

    void intToDouble2(@Unsigned int ui) {
        // :: error: (assignment.type.incompatible)
        double d = ui;
    }

    double intToDouble3(@Unsigned int ui) {
        // :: error: (return.type.incompatible)
        return ui;
    }
}
