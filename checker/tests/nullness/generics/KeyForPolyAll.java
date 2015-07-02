package nullness.generics;

import org.checkerframework.checker.nullness.qual.*;
import org.checkerframework.framework.qual.PolyAll;

import java.util.*;

//@skip-test
class KeyForPolyAll
{
    Map<@KeyFor("dict") String, String> dict = new HashMap<@KeyFor("dict") String, String>();
    void m() {
        Set<@KeyFor("dict") String> s = nounSubset(dict.keySet());

        for (@KeyFor("dict") String noun : nounSubset(dict.keySet())) { }
    }

    // This method's declaration uses no @KeyFor annotations
    // because in addition to being used by the dictionary feature,
    // it is also used by a spell checker that only stores sets of words
    // and does not use the notions of dictionaries, maps or keys.
    Set<@PolyAll String> nounSubset(Set<@PolyAll String> words) { return words; }
}
