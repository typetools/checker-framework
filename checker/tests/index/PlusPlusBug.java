import org.checkerframework.checker.index.qual.*;

public class PlusPlusBug {
    int[] array = {};

    void test(@LTLengthOf("array") int x) {
        //TODO: This should issue an error
        ////:: error: (compound.assignment.type.incompatible)
        x++;
        //:: error: (compound.assignment.type.incompatible)
        ++x;
        //:: error: (assignment.type.incompatible)
        x = x + 1;
    }
}
