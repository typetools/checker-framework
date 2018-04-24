import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyUse {
    void TestList(@Det ArrayList<@Det Integer> myDetList, @NonDet int rand) {
        // :: error: (argument.type.incompatible)
        myDetList.add(rand, 50);
    }
}
