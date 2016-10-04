import java.util.*;
import org.checkerframework.checker.minlen.qual.*;

class ListSupport {

    void newListMinLen() {
        List<Integer> list = new ArrayList<Integer>();

        //:: error: (assignment.type.incompatible)
        @MinLen(1) List<Integer> list2 = list;

        @MinLen(0) List<Integer> list3 = list;
    }
}
