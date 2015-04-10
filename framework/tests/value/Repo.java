import org.checkerframework.common.value.qual.*;
import java.util.BitSet;


class Binaries {
    private BitSet bitmap;
    boolean flag = true;

    void testLoop() {
        for (int i = 0; i < 20; i++) {
            //:: error: (assignment.type.incompatible)
            @IntVal(0) int x = i;
            //TODO: this is a dataflow bug. (March, 6, 2015)
            //:: error: (conditional.type.incompatible)
            int j = flag ? i : 3;
        }
    }
}
