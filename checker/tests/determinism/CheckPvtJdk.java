package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

public class CheckPvtJdk {
    void newList() {
        List<Integer> lst = new ArrayList<Integer>(new ArrayList<Integer>());
    }
}
