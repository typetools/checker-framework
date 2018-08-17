package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

// @skip-test
class TestArrayUpperBounds {
    public static <@Det T extends @PolyDet Object> T[] newArray() {
        T[] arr = (T[]) new Object[0];
        return arr;
    }

    public static <T> T[] newArray1() {
        T[] arr = (T[]) new Integer[10];
        return arr;
    }

    public static <T> T justReturn(T ret) {
        T val = ret;
        return val;
    }
}
