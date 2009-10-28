// This code is illegal (javac issues an error), but nonetheless the
// checkers shouldn't crash.  (Maybe they shouldn't run at all if javac
// issues any errors?)

import checkers.interning.quals.*;

import java.util.*;

public class DontCrash {

    // from VarInfoAux
    static class VIA {
        private static VIA theDefault = new VIA();
        private Map<@Interned String, @Interned String> map;

        void testMap() {
            Map<@Interned String,@Interned String> mymap;
            mymap = theDefault.map;
            mymap = new HashMap<@Interned String,@Interned String>(theDefault.map);
        }
    }

}
