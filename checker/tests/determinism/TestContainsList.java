import java.util.ArrayList;
import org.checkerframework.checker.determinism.qual.*;

public class TestContainsList {
    void TestList(@Det ArrayList<@Det Integer> myDetList, @NonDet Integer rand) {
        @Det boolean ret;
        // :: error: (assignment.type.incompatible)
        ret = myDetList.contains(rand);
    }

    void TestList1(
            @Det ArrayList<@Det Integer> myDetList, @NonDet ArrayList<@NonDet Integer> rand) {
        @Det boolean ret;
        // :: error: (assignment.type.incompatible)
        ret = myDetList.contains(rand);
    }

    void TestList2(@Det ArrayList<@Det Integer> myDetList, @NonDet int rand) {
        @Det boolean ret;
        // :: error: (assignment.type.incompatible)
        ret = myDetList.contains(rand);
    }

        void TestList3(@NonDet int rand) {
            @NonDet Integer ndInt = new Integer(rand);
        }

        void TestList4(@NonDet Integer elem) {
            @NonDet ArrayList<@NonDet Integer> arrL = new @NonDet ArrayList<@NonDet Integer>(elem);
        }

    public @PolyDet TestContainsList(@PolyDet int a) {}

    void TestList4(@NonDet int r) {
        @NonDet TestContainsList t = new TestContainsList(r);
    }
}
