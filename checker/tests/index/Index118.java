import org.checkerframework.checker.index.qual.*;
import org.checkerframework.common.value.qual.*;

public class Index118 {

    public static void foo(String @ArrayLen(4) [] args) {
        for (int i = 1; i <= 3; i++) {
            @IntRange(from = 1, to = 3) int x = i;
            System.out.println(args[i]);
        }
    }

    public static void bar(@NonNegative int i, String @ArrayLen(4) [] args) {
        if (i <= 3) {
            System.out.println(args[i]);
        }
    }
}
