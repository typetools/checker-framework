import java.util.TreeSet;
import org.checkerframework.checker.nullness.qual.Nullable;

class TreeSetTest {

    public static void main(String[] args) {

        //:: error: (type.argument.type.incompatible)
        TreeSet<@Nullable Integer> ts = new TreeSet<>();

        ts.add(null);
    }
}
