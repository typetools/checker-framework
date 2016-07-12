import java.util.*;
import tests.util.*;

public class Wildcards {
    void process(List<? extends Date> arg) {}

    void test() {
        List<? extends @Odd Date> myList = null;
        process(myList);
    }
}
