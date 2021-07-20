import org.checkerframework.checker.nullness.qual.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public final class MissingBoundAnnotations {

    public static <K extends Comparable<? super K>, V> Collection<@KeyFor("#1") K> sortedKeySet(
            Map<K, V> m) {
        ArrayList<@KeyFor("m") K> theKeys = new ArrayList<>(m.keySet());
        Collections.sort(theKeys);
        return theKeys;
    }

    public static <K extends Comparable<? super K>, V>
            Collection<@KeyFor("#1") K> sortedKeySetSimpler(ArrayList<@KeyFor("#1") K> theKeys) {
        Collections.sort(theKeys);
        return theKeys;
    }
}
