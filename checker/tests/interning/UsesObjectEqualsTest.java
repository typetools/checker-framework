import java.util.LinkedList;
import java.util.prefs.*;
import org.checkerframework.checker.interning.qual.Interned;
import org.checkerframework.checker.interning.qual.UsesObjectEquals;

public class UsesObjectEqualsTest {

    public @UsesObjectEquals class A {
        public A() {}
    }

    @UsesObjectEquals
    class B extends A {}

    class B2 extends A {}

    // changed to inherited, no (superclass.annotated) warning
    class C extends A {}

    class D {}

    @UsesObjectEquals
    // :: error: (superclass.notannotated)
    class E extends D {}

    @UsesObjectEquals
    // :: error: (overrides.equals)
    class TestEquals {

        @org.checkerframework.dataflow.qual.Pure
        public boolean equals(Object o) {
            return true;
        }
    }

    class TestComparison {

        public void comp(@Interned Object o, A a1, A a2) {
            if (a1 == a2) {
                System.out.println("one");
            }
            if (a1 == o) {
                System.out.println("two");
            }
            if (o == a1) {
                System.out.println("three");
            }
        }
    }

    @UsesObjectEquals
    class ExtendsInner1 extends UsesObjectEqualsTest.A {}

    class ExtendsInner2 extends UsesObjectEqualsTest.A {}

    class MyList extends LinkedList {}
}
