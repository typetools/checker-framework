
import org.checkerframework.checker.minlen.qual.*;

public class LUB {

    public static void MinLen(int @MinLen(10) [] arg, int @MinLen(4) [] arg2) {
        int[] arr;
        if (true) {
            arr = arg;
        } else {
            arr = arg2;
        }
        //:: error: (assignment.type.incompatible)
        int @MinLen(10) [] res = arr;
        int @MinLen(4) [] res2 = arr;
        //:: error: (assignment.type.incompatible)
        int @MinLenBottom [] res3 = arr;
    }

    public static void Bottom(int @MinLenBottom [] arg, int @MinLen(4) [] arg2) {
        int[] arr;
        if (true) {
            arr = arg;
        } else {
            arr = arg2;
        }
        //:: error: (assignment.type.incompatible)
        int @MinLen(10) [] res = arr;
        int @MinLen(4) [] res2 = arr;
        //:: error: (assignment.type.incompatible)
        int @MinLenBottom [] res3 = arr;
    }

    public static void BothBottom(int @MinLenBottom [] arg, int @MinLenBottom [] arg2) {
        int[] arr;
        if (true) {
            arr = arg;
        } else {
            arr = arg2;
        }
        int @MinLen(10) [] res = arr;
        int @MinLenBottom [] res2 = arr;
    }
}
