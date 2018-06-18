import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestListObject {
    //    void testContains1(@Det List<@Det Integer> lst, @Det Integer obj){
    //        @Det boolean res = lst.contains(obj);
    //    }
    //    void testContains2(@OrderNonDet List<@Det Integer> lst, @Det Integer obj){
    //        @Det boolean res = lst.contains(obj);
    //    }
    //    void testContains3(@NonDet List<@Det Integer> lst, @Det Integer obj){
    //        // :: error: (assignment.type.incompatible)
    //        @Det boolean res = lst.contains(obj);
    //    }
    //    void testContains4(@Det List<@Det List<@Det Integer>> lst, @Det List<@Det Integer> obj){
    //        @Det boolean res = lst.contains(obj);
    //    }
    //    void testContains5(@OrderNonDet List<@Det List<@Det Integer>> lst, @Det List<@Det Integer> obj){
    //        @Det boolean res = lst.contains(obj);
    //    }
    void testContains6(
            @OrderNonDet List<@OrderNonDet List<@Det Integer>> lst,
            @OrderNonDet List<@Det Integer> obj) {
        @Det boolean res = lst.contains(obj);
    }
    //    void testContains7(@OrderNonDet List<@OrderNonDet List<@Det Integer>> lst, @NonDet List<@Det Integer> obj){
    //        @Det boolean res = lst.contains(obj);
    //    }
}
