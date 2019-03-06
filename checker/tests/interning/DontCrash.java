// This code is illegal (javac issues an error), but nonetheless the
// org.checkerframework.checker shouldn't crash.  (Maybe they shouldn't run at all if javac
// issues any errors?)
// @skip-test

import java.util.HashMap;
import java.util.Map;
import org.checkerframework.checker.interning.qual.Interned;

public class DontCrash {

    // from VarInfoAux
    static class VIA {
        // :: non-static variable this cannot be referenced from a static context
        // :: inner classes cannot have static declarations
        // :: non-static variable this cannot be referenced from a static context
        // :: inner classes cannot have static declarations
        private static VIA theDefault = new VIA();
        private Map<@Interned String, @Interned String> map;

        void testMap() {
            Map<@Interned String, @Interned String> mymap;
            mymap = theDefault.map;
            mymap = new HashMap<@Interned String, @Interned String>(theDefault.map);
            mymap = new HashMap<>(theDefault.map);
        }
    }
}
