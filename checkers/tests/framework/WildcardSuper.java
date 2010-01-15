import checkers.util.test.*;
import java.util.*;

class WildcardSuper {
    void test(List<? super @Odd String> list) {
        //:: (type.incompatible)
        @Odd Object odd = list.get(0);
    }
}