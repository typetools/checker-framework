import java.util.BitSet;
import org.checkerframework.common.value.qual.*;

class Repo {
    private BitSet bitmap;
    boolean flag = true;

    void testLoop() {
        for (int i = 0; i < 20; i++) {
            // :: error: (assignment.type.incompatible)
            @IntVal(0) int x = i;
            int j = flag ? i : 3;
        }
    }
}
