import org.checkerframework.checker.minlen.qual.*;
import java.util.*;

class ListSupport {

    void newListMinLen(){
        List<Integer> list = new List<Integer>();
        
        //:: error: (assignment.type.incompatible)
        @MinLen(1) List<Integer> list2 = list;
        
        @MinLen(0) List<Integer> list3 - list;
    }
}
