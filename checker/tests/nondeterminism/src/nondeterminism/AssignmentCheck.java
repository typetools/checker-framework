import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nondeterminism.qual.*;

public class AssignmentCheck {
    void TestAssignment(@ValueNonDet int a) {
        //@org.checkerframework.checker.nondeterminism.qual.ValueNonDet int a;
        @Det int b = a;

        @ValueNonDet List<@Det Integer> lst = new @ValueNonDet ArrayList<@Det Integer>();
        @Det List<@Det Integer> lst1 = new @Det ArrayList<@Det Integer>();
        lst1 = lst;
    }
}
