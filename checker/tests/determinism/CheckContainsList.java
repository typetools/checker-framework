import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.determinism.qual.*;

public class CheckContainsList {
    void checkContains(@NonDet Object o) {
        @Det List<@Det Integer> lst = new @Det ArrayList<@Det Integer>();
        for (int i = 0; i < 6; i++) {
            lst.add(i);
        }
        // :: error: (argument.type.incompatible)
        System.out.println(lst.contains(o));
    }

    void CheckContains1(
            @OrderNonDet ArrayList<@Det Integer> lst1, @NonDet ArrayList<@Det Integer> o) {
        // :: error: (argument.type.incompatible)
        System.out.println(lst1.contains(o));
    }

    void CheckContains2(@OrderNonDet ArrayList<@Det Integer> lst1, @Det ArrayList<@Det Integer> o) {
        System.out.println(lst1.contains(o));
    }
}
