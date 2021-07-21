package nullness.generics;

import org.checkerframework.checker.nullness.qual.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

// test related to issue 429: https://github.com/typetools/checker-framework/issues/429
public class KeyForPolyKeyFor {
    // TODO: Figure out why diamond operator does not work:
    // Map<@KeyFor("dict") String, String> dict = new HashMap<>();
    Map<@KeyFor("dict") String, String> dict = new HashMap<@KeyFor("dict") String, String>();

    void m() {
        Set<@KeyFor("dict") String> s = nounSubset(dict.keySet());

        for (@KeyFor("dict") String noun : nounSubset(dict.keySet())) {}
    }

    // This method's declaration uses no @KeyFor annotations because in addition to being used by
    // the dictionary feature, it is also used by a spell checker that only stores sets of words and
    // does not use the notions of dictionaries, maps or keys.
    Set<@PolyKeyFor String> nounSubset(Set<@PolyKeyFor String> words) {
        return words;
    }
}
