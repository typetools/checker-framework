import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestPolyConstructor {
    void createArrayList(@Det int i) {
        ArrayList<Integer> arList = new ArrayList<Integer>(i);
        System.out.println(arList);
    }

    void createArrayList1(@NonDet ArrayList<@NonDet Integer> c) {
        // :: error: (argument.type.incompatible)
        new ArrayList<Integer>(c);
    }

    void trimArrayList(@Det ArrayList<@Det String> arList) {
        arList.trimToSize();
    }
}
