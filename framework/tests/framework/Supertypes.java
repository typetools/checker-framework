import java.util.ArrayList;
import java.util.List;
import testlib.util.*;

public class Supertypes {
    static interface Inter<E> {}

    static class A extends ArrayList<String> implements Inter<@Odd String> {}

    static class B extends ArrayList<@Odd String> implements Inter<String> {}

    A a1;
    @Odd A a2;

    B b1;
    @Odd B b2;

    void testSelf() {
        // :: error: (assignment.type.incompatible)
        @Odd A t1 = a1; // should emit error
        @Odd A t2 = a2;
        // :: error: (assignment.type.incompatible)
        @Odd B t3 = b1; // should emit error
        @Odd B t4 = b2;
    }

    void testList() {
        List<String> l1 = a1;
        List<String> l2 = a2;
        // :: error: (assignment.type.incompatible)
        List<String> l3 = b1; // should emit error
        // :: error: (assignment.type.incompatible)
        List<String> l4 = b2; // should emit error

        // :: error: (assignment.type.incompatible)
        List<@Odd String> l5 = a1; // should emit error
        // :: error: (assignment.type.incompatible)
        List<@Odd String> l6 = a2; // should emit error
        List<@Odd String> l7 = b1;
        List<@Odd String> l8 = b2;
    }

    void testInter() {
        // :: error: (assignment.type.incompatible)
        Inter<String> l1 = a1; // should emit error
        // :: error: (assignment.type.incompatible)
        Inter<String> l2 = a2; // should emit error
        Inter<String> l3 = b1;
        Inter<String> l4 = b2;

        Inter<@Odd String> l5 = a1;
        Inter<@Odd String> l6 = a2;
        // :: error: (assignment.type.incompatible)
        Inter<@Odd String> l7 = b1; // should emit error
        // :: error: (assignment.type.incompatible)
        Inter<@Odd String> l8 = b2; // should emit error
    }

    void testListOp() {
        String s1 = a1.get(0);
        String s2 = a2.get(0);
        String s3 = b1.get(0);
        String s4 = b2.get(0);

        // :: error: (assignment.type.incompatible)
        @Odd String s5 = a1.get(0); // should emit error
        // :: error: (assignment.type.incompatible)
        @Odd String s6 = a2.get(0); // should emit error
        @Odd String s7 = b1.get(0);
        @Odd String s8 = b2.get(0);
    }

    void ListIterable() {
        for (String s : a1) ;
        for (String s : a2) ;
        for (String s : b1) ;
        for (String s : b2) ;

        // :: error: (enhancedfor.type.incompatible)
        for (@Odd String s : a1) ; // should emit error
        // :: error: (enhancedfor.type.incompatible)
        for (@Odd String s : a2) ; // should emit error
        for (@Odd String s : b1) ;
        for (@Odd String s : b2) ;
    }
}
