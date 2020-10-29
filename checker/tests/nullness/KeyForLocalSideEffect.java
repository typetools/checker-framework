import java.util.HashMap;
import org.checkerframework.checker.nullness.qual.*;

public class KeyForLocalSideEffect {

    String k = "key";
    HashMap<String, Integer> m = new HashMap<>();

    void testContainsKeyForFieldKeyAndLocalMap() {
        HashMap<String, Integer> m_local = m;

        if (m_local.containsKey(k)) {
            @KeyFor("m_local") String s = k;
            havoc();
            @NonNull Integer val = m_local.get(s);
        }
    }

    void havoc() {
        m = new HashMap<>();
    }
}
