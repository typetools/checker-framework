import java.util.*;
import org.checkerframework.checker.minlen.qual.*;

class ListAddInfiniteLoop {

    void ListLoop(List<Integer> list) {
        while (true) {
            list.add(4);
        }
    }
}
