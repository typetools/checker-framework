import tests.util.*;
import java.util.*;

class WildcardSuper {
    void test(List<? super @Odd String> list) {
        //:: error: (assignment.type.incompatible)
        @Odd Object odd = list.get(0);
    }
}
