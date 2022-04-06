import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

class NestedAnonymous {
    Object o1 = new @Tainted Object() {};
    Object o2 = new Outer.@Tainted Inner() {};

    // :: error: (assignment.type.incompatible)
    Outer.@Untainted Inner unt = new Outer.@Tainted Inner() {};

    static class Outer {
        static class Inner {}
    }
}
