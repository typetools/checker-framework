// Based on a false positive encountered in Daikon related to common CFGs
// for subcheckers. This shows the original version of the Daikon code,
// with an expected error due to missing standardization. See DaikonEnhancedFor.java
// for the "fixed" version.

import java.util.*;
import org.checkerframework.checker.nullness.qual.*;

class DaikonEnhancedForNoThis {

    @SuppressWarnings("nullness")
    Map<Object, Set<@KeyFor("cmap") Object>> cmap = null;

    @SuppressWarnings("nullness")
    Object[] getObjects() {
        return null;
    }

    void process(@KeyFor("this.cmap") Object super_c) {
        @SuppressWarnings("keyfor") // the loop below makes all these keys to cmap
        @KeyFor("cmap") Object[] clazzes = getObjects();
        // go through all of the classes and intialize the map
        for (Object cd : clazzes) {
            cmap.put(cd, new TreeSet<@KeyFor("cmap") Object>());
        }
        // go through the list again and put in the derived class information
        for (Object cd : clazzes) {
            Set<@KeyFor("this.cmap") Object> derived = cmap.get(super_c);
            // In this version of the test, the type of cd is @KeyFor("cmap") Object,
            // because it was not standardized during CFG construction.
            // :: error: argument.type.incompatible
            derived.add(cd);
        }
    }
}
