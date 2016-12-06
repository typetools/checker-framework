import java.util.*;
import org.checkerframework.checker.upperbound.qual.*;

class ViewpointAdaptTest {

    void ListGet(
            @LTLengthOf("list") int index, @LTEqLengthOf("list") int notIndex, List<Integer> list) {

        // should be fine
        list.set(index, 4);

        //:: error: (argument.type.incompatible)
        list.set(notIndex, 4);
    }
}
