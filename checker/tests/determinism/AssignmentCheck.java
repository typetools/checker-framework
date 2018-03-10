import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.determinism.qual.*;

public class AssignmentCheck {
    void TestAssignment(@NonDet int a) {
        //@org.checkerframework.checker.determinism.qual.NonDet int a;
        @Det int b = a;

        @NonDet List<@Det Integer> lst = new @NonDet ArrayList<@Det Integer>();
        @Det List<@Det Integer> lst1 = new @Det ArrayList<@Det Integer>();
        lst1 = lst;
    }
}
