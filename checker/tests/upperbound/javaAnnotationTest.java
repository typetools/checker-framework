import java.util.*;
import org.checkerframework.checker.upperbound.qual.*;

class ListSupport {

    void ListGet(@LessThanLength("list") int index, @LessThanOrEqualToLength("list") int notIndex, List<Integer> list) {
        list.get(index);

        //:: error: (argument.type.incompatible)
        list.get(notIndex);
    }
}
