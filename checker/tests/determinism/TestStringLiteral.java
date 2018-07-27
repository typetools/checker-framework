package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;
import org.checkerframework.checker.index.qual.*;

class TestStringLiteral {
    void testStr() {
        /*@SameLen("a")*/ int[] a;
    }
}
