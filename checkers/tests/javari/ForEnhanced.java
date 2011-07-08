import java.util.*;
import checkers.javari.quals.*;

class ForEnhanced {

    @Mutable List<Object> mm = new LinkedList<Object>();
    @Mutable List<@ReadOnly Object> mr = new LinkedList<@ReadOnly Object>();
    @ReadOnly List<Object> rm = mm;
    @ReadOnly List<@ReadOnly Object> rr = mr;
    Object[] mma;
    @ReadOnly Object[] mra;
    Object @ReadOnly [] rma;
    @ReadOnly Object @ReadOnly [] rra;

    @ReadOnly List<@ReadOnly List<Object>> rrm;

    void testMutable() {
        for (Object o : mm);
        //:: error: (enhancedfor.type.incompatible)
        for (Object o : mr);  // error
        for (Object o : rm);
        //:: error: (enhancedfor.type.incompatible)
        for (Object o : rr);  // error
        for (@ReadOnly Object o : mm);
        for (@ReadOnly Object o : mr);
        for (@ReadOnly Object o : rm);
        for (@ReadOnly Object o : rr);

        for (Object o : mma);
        //:: error: (enhancedfor.type.incompatible)
        for (Object o : mra);  // error
        for (Object o : rma);
        //:: error: (enhancedfor.type.incompatible)
        for (Object o : rra);  // error
        for (@ReadOnly Object o : mma);
        for (@ReadOnly Object o : mra);
        for (@ReadOnly Object o : rma);
        for (@ReadOnly Object o : rra);

        //:: error: (enhancedfor.type.incompatible)
        for (List<@ReadOnly Object> w : rrm);   // error

        //:: error: (enhancedfor.type.incompatible)
        for (@ReadOnly List<@ReadOnly Object> rr2 : rrm) { // error
            //:: error: (enhancedfor.type.incompatible)
            for (Object no : rr2);                // error
            for (@ReadOnly Object yes : rr2);
        }

        for (@ReadOnly List<Object> rm2 : rrm) {
            for (Object yes : rm2);
            for (@ReadOnly Object yes : rm2);
        }

    }

    public @Mutable List<@ReadOnly Object> getMR() { return mr; }
    public @ReadOnly Object @ReadOnly [] getRRA() { return rra; }
    public Object[] getMMA() { return mma; }

    void testMethods() {
        //:: error: (enhancedfor.type.incompatible)
        for (Object o : getMR()); //error
        for (@ReadOnly Object o : getMR());

        //:: error: (enhancedfor.type.incompatible)
        for (Object o : getRRA()); // error
        for (@ReadOnly Object o : getRRA());

        for (Object o : getMMA());
    }

    /* Delete this line and commented lines, and add the following errors:
//ForEnhanced.java:72: cannot assign a ReadOnly expression to a Mutable variable
//ForEnhanced.java:75: cannot assign a ReadOnly expression to a Mutable variable
//ForEnhanced.java:81: cannot assign a ReadOnly expression to a Mutable variable
//ForEnhanced.java:87: cannot assign a ReadOnly expression to a Mutable variable
//ForEnhanced.java:88: cannot assign a ReadOnly expression to a Mutable variable
//ForEnhanced.java:89: cannot assign a ReadOnly expression to a Mutable variable
    class StringList extends LinkedList<@ReadOnly String> { };
    class StringIterable implements Iterable<@ReadOnly String> {
        public Iterator<@ReadOnly String> iterator() { return null; }
    }

    // Test more iterables
    void testIterables() {
        StringList l1 = new StringList();
        for (String s : l1);    // error

        StringIterable l2 = new StringIterable();
        for (String s : l2);    // error
    }

    // Test Expressions without Elements

    void testConditionalExpress() {
        for (Object o : true ? getRRA() : getMMA()); // error
        for (@ReadOnly Object o : true ? getRRA() : getMMA());
    }


    void testNewObjects() {
        for (String str : new ArrayList<@ReadOnly String>());   // error
        for (String str : new StringList());    // error
        for (String str : new StringIterable());    //error
    }
    */
}
