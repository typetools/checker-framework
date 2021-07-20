// Based on a false positive encountered in Daikon related to common CFGs
// by the KeyFor checker.

import org.checkerframework.checker.nullness.qual.*;

import java.util.*;

class DaikonEnhancedFor {

    @SuppressWarnings("nullness")
    Map<Object, Set<@KeyFor("cmap") Object>> cmap = null;

    @SuppressWarnings("nullness")
    Object[] getObjects() {
        return null;
    }

    void process(@KeyFor("this.cmap") Object super_c) {
        @SuppressWarnings("keyfor") // the loop below makes all these keys to cmap
        @KeyFor("this.cmap") Object[] clazzes = getObjects();
        // go through all of the classes and intialize the map
        for (Object cd : clazzes) {
            cmap.put(cd, new TreeSet<@KeyFor("cmap") Object>());
        }
        // go through the list again and put in the derived class information
        for (Object cd : clazzes) {
            Set<@KeyFor("this.cmap") Object> derived = cmap.get(super_c);
            derived.add(cd);
        }
    }
}
