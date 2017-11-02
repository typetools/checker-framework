import java.util.Arrays;
import java.util.List;
import org.checkerframework.common.value.qual.MinLen;

// @skip-test until we bring list support back

public class ArrayAsList {

    public static void toList(Integer @MinLen(10) [] arg) {
        @MinLen(10) List<Integer> list = Arrays.asList(arg);
        System.out.println("Integer: " + list.size());
    }

    public static void toList2(int @MinLen(10) [] arg2) {
        // :: error: (assignment.type.incompatible)
        @MinLen(10) List list = Arrays.asList(arg2);
        System.out.println("int: " + list.size());

        @MinLen(1) List list2 = Arrays.asList(arg2);
    }

    public static void toList3() {
        @MinLen(4) List<Integer> list = Arrays.asList(1, 2, 3, 6);
        System.out.println("args: " + list.size());
    }
}
