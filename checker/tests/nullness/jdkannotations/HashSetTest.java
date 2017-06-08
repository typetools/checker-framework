import java.util.HashSet;
import org.checkerframework.checker.nullness.qual.Nullable;

class HashSetTest {

    public static void main(String[] args) {

        //:: error: (type.argument.type.incompatible)
        HashSet<@Nullable Integer> hs = new HashSet<>();

        hs.add(null);
    }
}
