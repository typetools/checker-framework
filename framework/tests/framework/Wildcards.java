import org.checkerframework.framework.testchecker.util.*;

import java.util.Date;
import java.util.List;

public class Wildcards {
    void process(List<? extends Date> arg) {}

    void test() {
        List<? extends @Odd Date> myList = null;
        process(myList);
    }
}
