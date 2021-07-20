import org.checkerframework.checker.nullness.qual.*;

import java.util.HashMap;
import java.util.Vector;

public class KeyForFlow extends HashMap<String, Object> {

    String k = "key";
    HashMap<String, Object> m = new HashMap<>();

    void testContainsKeyForLocalKeyAndLocalMap() {
        String k_local = "key";
        HashMap<String, Object> m_local = new HashMap<>();

        if (m_local.containsKey(k_local)) {
            @KeyFor("m_local") Object s = k_local;
        }

        // :: error: (assignment.type.incompatible)
        @KeyFor("m_local") String s2 = k_local;
    }

    void testContainsKeyForLocalKeyAndFieldMap() {
        String k_local = "key";

        if (m.containsKey(k_local)) {
            @KeyFor("m") Object s = k_local;
        }

        // :: error: (assignment.type.incompatible)
        @KeyFor("m") String s2 = k_local;
    }

    void testContainsKeyForFieldKeyAndLocalMap() {
        HashMap<String, Object> m_local = new HashMap<>();

        if (m_local.containsKey(k)) {
            @KeyFor("m_local") Object s = k;
        }

        // :: error: (assignment.type.incompatible)
        @KeyFor("m_local") String s2 = k;
    }

    void testContainsKeyForFieldKeyAndFieldMap() {
        if (m.containsKey(k)) {
            @KeyFor("m") Object s = k;
        }

        // :: error: (assignment.type.incompatible)
        @KeyFor("m") String s2 = k;
    }

    static String k_s = "key";

    void testContainsKeyForStaticKeyAndFieldMap() {
        if (m.containsKey(k_s)) {
            @KeyFor("m") Object s = k_s;
        }

        // :: error: (assignment.type.incompatible)
        @KeyFor("m") String s2 = k_s;
    }

    static HashMap<String, Object> m_s = new HashMap<>();

    void testContainsKeyForFieldKeyAndStaticMap() {
        if (m_s.containsKey(k)) {
            // Currently for this to work, the user must write @KeyFor("classname.static_field")
            @KeyFor("m_s") Object s = k;
        }

        // :: error: (assignment.type.incompatible)
        @KeyFor("m_s") String s2 = k;
    }

    void testContainsKeyForFieldKeyAndReceiverMap() {
        if (containsKey(k)) {
            @KeyFor("this") Object s = k;
        }

        // :: error: (assignment.type.incompatible)
        @KeyFor("this") String s2 = k;
    }

    // TODO: The diamond operator does not work here:
    //    Vector<@KeyFor("m2") String> coll = new Vector<>();
    // Figure out why not.
    Vector<@KeyFor("m2") String> coll = new Vector<@KeyFor("m2") String>();
    HashMap<String, Object> m2 = new HashMap<>();
    String k2 = "key2";

    void testCallingPutAfterAdd() {
        // :: error: (argument.type.incompatible)
        coll.add(k2);
        m2.put(k2, new Object());
    }

    void testPutForLocalKeyAndLocalMap() {
        HashMap<String, Object> m2_local = new HashMap<>();
        Vector<@KeyFor("m2_local") String> coll_local = new Vector<>();
        String k2_local = "key2";

        m2_local.put(k2_local, new Object());
        coll_local.add(k2_local);
    }

    void testPutForLocalKeyAndFieldMap() {
        String k2_local = "key2";

        m2.put(k2_local, new Object());
        coll.add(k2_local);
    }

    void testPutForFieldKeyAndLocalMap() {
        HashMap<String, Object> m2_local = new HashMap<>();
        Vector<@KeyFor("m2_local") String> coll_local = new Vector<>();

        m2_local.put(k2, new Object());
        coll_local.add(k2);
    }

    void testPutForFieldKeyAndFieldMap() {
        m2.put(k2, new Object());
        coll.add(k2);
    }

    /*
    This scenario is not working since in Vector, "this" gets translated to "coll_local".
    The same thing happens if the collection is a field instead of a local.
    However this seems like a low-priority scenario to enable.

    void testPutForFieldKeyAndReceiverMap() {
      Vector<@KeyFor("this") String> coll_local = new Vector<>();

      put(k2, new Object());
      coll_local.add(k2);
    }*/

    class foo {
        public HashMap<String, Object> m = new HashMap<>();
    }

    void testContainsKeyForFieldKeyAndMapFieldOfOtherClass() {
        foo f = new foo();

        if (f.m.containsKey(k)) {
            @KeyFor("f.m") Object s = k;
        }

        // :: error: (assignment.type.incompatible)
        @KeyFor("f.m") String s2 = k;
    }

    void testPutForFieldKeyAndMapFieldOfOtherClass() {
        foo f = new foo();
        Vector<@KeyFor("f.m") String> coll_local = new Vector<>();
        f.m.put(k2, new Object());
        coll_local.add(k2);
    }

    /*public void testAddToListInsteadOfMap(List<@KeyFor("#4") String> la, String b, @KeyFor("#4") String c, Map<String, String> a) {
      // Disabled error (assignment.type.incompatible)
      List<String> ls1 = la;
      List<@KeyFor("#4") String> ls2 = la;
      ls1.add(b);
      // Disabled error (argument.type.incompatible)
      la.add(b);
      ls2.add(c);
      la.add(c);
      @NonNull String astr = a.get(ls2.get(0));
    }*/
}
