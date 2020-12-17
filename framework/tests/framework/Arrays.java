import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.checkerframework.framework.testchecker.util.*;

public class Arrays {
    Object[] @Odd [] objB1 = new Object[] @Odd [] {};
    Object[][] @Odd [] objB1a = new Object[][] @Odd [] {};
    Object @Odd [][][] objB1b = new Object @Odd [][][] {};
    @Odd Object[][][] objB1c = new @Odd Object[][][] {};

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface A {}

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface B {}

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface C {}

    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface D {}

    class Cell<T> {}

    // (This part is actually for the parser, not the framework; it should
    // be moved to the JSR 308 compiler test suite eventually.)
    void test() {

        Object z = new @A String[] {};

        // 308 only:
        Cell<@D Object @C [] @B [] @A []> o1;

        // w/new:
        Object o2a = new @D Object @C [] @B [] @A [] {};
        Object o2b = new @D Object @C [1] @B [2] @A [3];

        // w/175:
        @D Object @C [] @B [] @A [] o3;
    }

    void moreTest() {
        // Assignments:

        String[] s = null;
        String[] t = null;

        s[0] = null;
        t[0] = null;

        (new String[1])[0] = null;
        (new String[1])[0] = null;

        (new String[] {"foo"})[0] = null;
        (new String[] {"foo"})[0] = null;
    }

    void test2() {

        Object[][] objA1 = new Object[][] {};
        Object[][] objA2 = new Object[1][2];
        Object[][] objA3 = new Object[1][];

        Object[] @Odd [] objB1 = new Object[] @Odd [] {};
        Object[] @Odd [] objB2 = new Object[1] @Odd [2];
        Object[] @Odd [] objB3 = new Object[1] @Odd [];

        Object @Odd [][] objC1 = new Object @Odd [][] {};
        Object @Odd [][] objC2 = new Object @Odd [1][2];
        Object @Odd [][] objC3 = new Object @Odd [1][];

        @Odd Object[][] objD1 = new @Odd Object[][] {};
        @Odd Object[][] objD2 = new @Odd Object[1][2];
        @Odd Object[][] objD3 = new @Odd Object[1][];

        Object @Odd [] @Odd [] objE1 = new Object @Odd [] @Odd [] {};
        Object @Odd [] @Odd [] objE2 = new Object @Odd [1] @Odd [2];
        Object @Odd [] @Odd [] objE3 = new Object @Odd [1] @Odd [];

        @Odd Object[] @Odd [] objF1 = new @Odd Object[] @Odd [] {};
        @Odd Object[] @Odd [] objF2 = new @Odd Object[1] @Odd [2];
        @Odd Object[] @Odd [] objF3 = new @Odd Object[1] @Odd [];

        @Odd Object @Odd [][] objG1 = new @Odd Object @Odd [][] {};
        @Odd Object @Odd [][] objG2 = new @Odd Object @Odd [1][2];
        @Odd Object @Odd [][] objG3 = new @Odd Object @Odd [1][];

        @Odd Object @Odd [] @Odd [] objH1 = new @Odd Object @Odd [] @Odd [] {};
        @Odd Object @Odd [] @Odd [] objH2 = new @Odd Object @Odd [1] @Odd [2];
        @Odd Object @Odd [] @Odd [] objH3 = new @Odd Object @Odd [1] @Odd [];
    }

    void test3() {
        @Odd Object o1 = new @Odd Object @Odd [] @Odd [] {};
        // :: error: (assignment.type.incompatible)
        @Odd Object o2 = new @Odd Object[] @Odd [] {}; // ERROR

        @Odd Object @Odd [] o3 = (new @Odd Object[] @Odd [] {})[0];
        // :: error: (assignment.type.incompatible)
        @Odd Object @Odd [] o4 = (new Object @Odd [][] {})[0]; // ERROR
        // :: error: (assignment.type.incompatible)
        @Odd Object @Odd [] o5 = (new @Odd Object[][] {})[0]; // ERROR

        Object @Odd [] o6 = (new Object[] @Odd [] {})[0];
        @Odd Object[] o7 = (new @Odd Object[][] {})[0];

        @Odd Object o8 = (new @Odd Object[][] {})[0][0];
    }

    void test4() {
        @Odd Object @Odd [] @Odd [] o1 = new @Odd Object @Odd [] @Odd [] {};
        @Odd Object @Odd [] @Odd [] @Odd [] o2 = new @Odd Object @Odd [1] @Odd [2] @Odd [3];
        @Odd Object @Odd [] @Odd [] o3 = new @Odd Object @Odd [1] @Odd [2] @Odd [];
        @Odd Object @Odd [] @Odd [] o4 = new @Odd Object @Odd [1] @Odd [] @Odd [];
    }

    void testInitializers() {
        //      @Odd String [] ara1 = { null, null };
        @Odd String[] ara2 = new @Odd String[] {null, null};

        //         // xx:: error: (assignment.type.incompatible)
        //        @Odd String [] arb1 = { null, "m" };
        // :: error: (array.initializer.type.incompatible)
        @Odd String[] arb2 = new @Odd String[] {null, "m"};
    }
}
