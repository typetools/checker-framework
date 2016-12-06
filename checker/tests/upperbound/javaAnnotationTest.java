import java.util.*;
import org.checkerframework.checker.upperbound.qual.*;

class ViewpointAdaptTest {
    /*
        void ListGet(
                @LTLengthOf("list") int index,
                @LTEqLengthOf("list") int notIndex,
                List<Integer> list) {
            ////:: error: (argument.type.incompatible)
            list.get(index);

            ////:: error: (argument.type.incompatible)
            list.get(notIndex);
        }
    */
}
