import java.util.List;
import org.checkerframework.dataflow.qual.Pure;
import org.checkerframework.framework.test.*;
import org.checkerframework.framework.testchecker.util.*;

public class Basic2 {

    // basic tests to make sure everything works
    void t1(@Odd String p1, String p2) {
        String l1 = p1;
        // :: error: (assignment.type.incompatible)
        @Odd String l2 = p2;
    }

    // basic local variable flow sensitivity
    void t2(@Odd String p1, String p2, boolean b1) {
        String l1 = p1;
        if (b1) {
            l1 = p2;
        }
        // :: error: (assignment.type.incompatible)
        @Odd String l3 = l1;

        l1 = p1;
        while (b1) {
            l1 = p2;
        }
        // :: error: (assignment.type.incompatible)
        @Odd String l4 = l1;
    }

    void t2b(@Odd String p1, String p2, @Odd String p3, boolean b1) {
        String l1 = p1;

        if (b1) {
            l1 = p3;
        }
        @Odd String l3 = l1;

        while (b1) {
            l1 = p3;
        }
        @Odd String l4 = l1;
    }

    // return statement
    void t3(@Odd String p1, String p2, boolean b1) {
        String l1 = p1;
        if (b1) {
            l1 = p2;
            return;
        }
        @Odd String l3 = l1;
    }

    // simple throw statement
    void t4(@Odd String p1, String p2, boolean b1) {
        String l1 = p1;
        if (b1) {
            l1 = p2;
            throw new RuntimeException();
        }
        @Odd String l3 = l1;
    }

    class C {
        C c;
        String f1, f2, f3;
        @Odd String g1, g2, g3;
    }

    // fields
    void t5(@Odd String p1, String p2, boolean b1, C c1, C c2) {
        c1.f1 = p1;
        @Odd String l1 = c1.f1;
        c2.f2 = p2; // assignment to f2 does not change knowledge about f1
        c1.f2 = p2;
        @Odd String l2 = c1.f1;
        c2.f1 = p2;
        // :: error: (assignment.type.incompatible)
        @Odd String l3 = c1.f1;
    }

    // fields
    void t6(@Odd String p1, String p2, boolean b1, C c1, C c2) {
        c1.f1 = p1;
        c1.c.f1 = p1;
        @Odd String l2 = c1.c.f1;
        c1.f1 = p2;
        // :: error: (assignment.type.incompatible)
        @Odd String l3 = c1.f1;

        c1.f1 = p1;
        c1.c.f1 = p1;
        @Odd String l4 = c1.c.f1;
        c2.c = c2;
        // :: error: (assignment.type.incompatible)
        @Odd String l5 = c1.f1;
        // :: error: (assignment.type.incompatible)
        @Odd String l6 = c1.c.f1;
    }

    // fields
    void t6b(@Odd String p1, String p2, boolean b1, C c1, C c2) {
        if (b1) {
            c1.f1 = p1;
        }
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = c1.f1;

        if (b1) {
            c1.f1 = p1;
        } else {
            c1.f1 = p1;
        }
        @Odd String l2 = c1.f1;
    }

    // method calls
    void nonpure() {}

    @Pure
    int pure() {
        return 1;
    }

    void t7(@Odd String p1, String p2, boolean b1, C c1, C c2) {
        c1.f1 = p1;
        nonpure();
        // :: error: (assignment.type.incompatible)
        @Odd String l1 = c1.f1;

        c1.f1 = p1;
        pure();
        @Odd String l2 = c1.f1;
    }

    // array accesses
    void t8(@Odd String a1[], String a2[], String p3) {
        String l1 = a1[0];

        // :: error: (assignment.type.incompatible)
        @Odd String l2 = a2[0];

        @Odd String l3 = l1;

        a1[0] = l1;
        a1[1] = l3;

        // :: error: (assignment.type.incompatible)
        a1[2] = p3;

        a2[0] = l1;
        a2[1] = l3;
        a2[2] = p3;
    }

    // self type
    void t9(@Odd Basic2 this) {
        @Odd Basic2 l1 = this;
    }

    // generics
    public <T extends String> void t10a(T p1, @Odd T p2) {
        T l1 = p1;
        p1 = l1;
        T l2 = p2;
        p2 = l2;
        // :: error: (assignment.type.incompatible)
        @Odd T l3 = p1;
        @Odd T l4 = p2;
    }

    public <T extends @Odd String> void t10b(T p1, @Odd T p2) {
        T l1 = p1;
        p1 = l1;
        T l2 = p2;
        p2 = l2;
        @Odd T l3 = p1;
        @Odd T l4 = p2;
    }

    // for-each loop
    void t11(@Odd String p1, String p2, List<String> list, List<@Odd String> oddList) {
        // :: error: (enhancedfor.type.incompatible)
        for (@Odd String i : list) {}
        for (@Odd String i : oddList) {
            @Odd String l1 = i;
        }
        for (@Odd String i : oddList) {
            // :: error: (assignment.type.incompatible)
            i = p2;
        }
        for (String i : oddList) {
            // @Odd String l3 = i;
        }
    }

    // cast without annotations
    void t12(@Odd String p1, String p2, boolean b1) {
        @Odd String l1 = (String) p1;
    }

    // final fields
    class CF {
        final String f1;
        CF c;

        void nonpure() {};

        CF(@Odd String p1) {
            f1 = p1;
            nonpure();
            @Odd String l1 = f1;
        }

        void CF_t1(@Odd String p1, CF o) {
            if (f1 == p1) {
                nonpure();
                @Odd String l1 = f1;
            }
        }
    }

    // final fields with initializer
    class A {
        final @Odd String f1 = null;
        final String f2 = f1;

        void A_t1() {
            @Odd String l1 = f2;
        }
    }
}
