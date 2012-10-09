import checkers.nullness.quals.*;

import java.util.*;

public class KeyForSubst {
    /*
    static class MyClass<T> {
        public T next() { return null; }
    }
    */

    @KeyFor("#0") String getMain(Object m) {
        throw new RuntimeException();
    }

    List<@KeyFor("#0") String> getDeep(Object m) {
        throw new RuntimeException();
    }

    @KeyFor("#0") List<@KeyFor("#1") String> getBoth(Object l, Object m) {
        throw new RuntimeException();
    }

    // OK, I think the annotation on the index is overdoing it, but it works.
    @KeyFor("#0") String @KeyFor("#1")[] getArray(Object l, Object m) {
        throw new RuntimeException();
    }


    public void testAssignMain(Object lastMap) {
        @KeyFor("lastMap") String key = getMain(lastMap);
    }

    public void testAssignDeep(Object lastMap) {
        List<@KeyFor("lastMap") String> key = getDeep(lastMap);
    }

    public void testAssignBoth(Object lastMap, Object newMap) {
        @KeyFor("lastMap") List<@KeyFor("newMap") String> key = getBoth(lastMap, newMap);
    }

    public void testAssignArray(Object lastMap, Object newMap) {
        @KeyFor("lastMap") String @KeyFor("newMap")[] key = getArray(lastMap, newMap);
    }

}
