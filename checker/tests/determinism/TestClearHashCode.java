package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class TestClearHashCode {
    void testClear(@Det List<@Det Integer> lst){
        lst.clear();
    }
    void testHash(@Det List<@Det Integer> lst){
        @NonDet int code = lst.hashCode();
    }
    void testClearOnd(@OrderNonDet List<@Det Integer> lst){
        lst.clear();
    }
    void testHashOnd(@OrderNonDet List<@Det Integer> lst){
        @NonDet int code = lst.hashCode();
    }
    void testClearNd(@NonDet List<@Det Integer> lst){
        lst.clear();
    }
    void testHashNd(@NonDet List<@NonDet Integer> lst){
        @NonDet int code = lst.hashCode();
    }
}
