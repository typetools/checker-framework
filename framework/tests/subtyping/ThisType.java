import org.checkerframework.framework.testchecker.util.*;

public class ThisType {
    void t1(@Encrypted ThisType this) {
        @Encrypted ThisType l1 = this;
        ThisType l2 = this;
        // Type of l2 is refined by flow -> legal
        l1 = l2;
    }

    void t2(ThisType this) {
        ThisType l1 = this;
        // :: error: (assignment.type.incompatible)
        @Encrypted ThisType l2 = this;
    }
}
