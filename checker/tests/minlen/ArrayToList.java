import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.minlen.qual.*;

class ArrayToList {

    public void toList(Integer @MinLen(10) [] arg) {
        @MinLen(10) List<Integer> list = Arrays.asList(arg);
    }
}
