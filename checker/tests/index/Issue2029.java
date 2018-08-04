import org.checkerframework.checker.index.qual.LessThan;
import org.checkerframework.checker.index.qual.NonNegative;

public class Issue2029 {
    void simpleMethod(@NonNegative @LessThan("#2") int index, @NonNegative int size, char val) {
        char[] arr = new char[size];
        arr[index] = val;
    }
}
