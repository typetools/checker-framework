package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
class TestArrayUpperBounds {
    public static <@Det T extends @PolyDet Object> T[] newArray() {
        T[] arr = (T[]) new Object[0];
        return arr;
    }
}
