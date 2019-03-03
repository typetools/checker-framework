package determinism;

import java.util.*;
import org.checkerframework.checker.determinism.qual.*;

class TestHashSet {
    void testConstructDefault() {
        // :: error: (assignment.type.incompatible)
        @Det Set<String> s = new HashSet<String>();
    }

    void testConstructCollection1(@Det List<@Det String> c) {
        // :: error: (argument.type.incompatible)
        System.out.println(new HashSet<@Det String>(c));
    }

    void testConstructCollection2(@PolyDet List<@Det String> c) {
        // :: error: (assignment.type.incompatible)
        @PolyDet Set<@Det String> s = new HashSet<@Det String>(c);
    }

    void testConstructCollection3(@Det List<@Det String> c) {
        @OrderNonDet Set<@Det String> s = new HashSet<@Det String>(c);
    }

    void testConstructCollection4(@PolyDet List<@Det String> c) {
        // :: error: (assignment.type.incompatible)
        @OrderNonDet Set<@Det String> s = new HashSet<@Det String>(c);
    }

    void testConstructCollection5(@PolyDet List<@Det String> c) {
        // :: error: (assignment.type.incompatible)
        @PolyDet Set<@Det String> s = new HashSet<@Det String>(c);
    }

    void testConstructCollection6(@PolyDet("up") List<@Det String> c) {
        // :: error: (assignment.type.incompatible)
        @PolyDet("up") Set<@Det String> s = new HashSet<@Det String>(c);
    }

    void testExplicitDet() {
        // :: error: (invalid.hash.set.constructor.invocation) :: warning:
        // (cast.unsafe.constructor.invocation)
        @OrderNonDet Set<String> s = new @Det HashSet<String>();
    }

    void testExplicitPoly() {
        // :: error: (invalid.hash.set.constructor.invocation)
        @NonDet Set<String> s = new @PolyDet HashSet<String>();
    }

    void testExplicitPolyUp() {
        // :: error: (invalid.hash.set.constructor.invocation)
        @NonDet Set<String> s = new @PolyDet("up") HashSet<String>();
    }

    void testIteration() {
        Set<String> s = new HashSet<String>();
        for (String str : s) {
            // :: error: (argument.type.incompatible)
            System.out.println(str);
        }
    }
}
