import checkers.nullness.quals.*;

import java.util.Map;
import java.util.HashMap;

public class KeyFors {

    public void withoutKeyFor() {
        Map<String, String> map = new HashMap<String, String>();
        String key = "key";

        //:: (type.incompatible)
        @NonNull String value = map.get(key);
    }

    public void withKeyFor() {
        Map<String, String> map = new HashMap<String, String>();
        @KeyFor("map") String key = "key";

        @NonNull String value = map.get(key);
    }
}

