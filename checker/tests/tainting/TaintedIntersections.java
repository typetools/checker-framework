import org.checkerframework.checker.tainting.qual.Tainted;
import org.checkerframework.checker.tainting.qual.Untainted;

public class TaintedIntersections {
    interface MyInterface {}

    void test1() {
        // null is @Untainted
        @Untainted Object o1 = (@Untainted Object & @Untainted MyInterface) null;
        // :: warning: (explicit.annotation.ignored)
        @Untainted Object o2 = (@Untainted Object & @Tainted MyInterface) null;
        // :: error: (assignment.type.incompatible) :: warning: (explicit.annotation.ignored)
        @Untainted Object o3 = (@Tainted Object & @Untainted MyInterface) null;
        // :: error: (assignment.type.incompatible)
        @Untainted Object o4 = (@Tainted Object & @Tainted MyInterface) null;
    }

    void test2() {
        // null is @Untainted
        @Untainted Object o1 = (@Untainted Object & MyInterface) null;
        @Untainted Object o3 = (Object & @Untainted MyInterface) null;
        // :: error: (assignment.type.incompatible)
        @Untainted Object o2 = (Object & @Tainted MyInterface) null;
        // :: error: (assignment.type.incompatible)
        @Untainted Object o4 = (@Tainted Object & MyInterface) null;
    }

    void test3(@Tainted MyInterface i) {
        // :: warning: (cast.unsafe)
        @Untainted Object o1 = (@Untainted Object & @Untainted MyInterface) i;
        // :: warning: (explicit.annotation.ignored) :: warning: (cast.unsafe)
        @Untainted Object o2 = (@Untainted Object & @Tainted MyInterface) i;
        // :: error: (assignment.type.incompatible) :: warning: (explicit.annotation.ignored)
        @Untainted Object o3 = (@Tainted Object & @Untainted MyInterface) i;
        // :: error: (assignment.type.incompatible)
        @Untainted Object o4 = (@Tainted Object & @Tainted MyInterface) i;
    }

    void test4(@Tainted MyInterface i) {
        // :: warning: (cast.unsafe)
        @Untainted Object o1 = (@Untainted Object & MyInterface) i;
        // :: warning: (cast.unsafe)
        @Untainted Object o3 = (Object & @Untainted MyInterface) i;
        // :: error: (assignment.type.incompatible)
        @Untainted Object o2 = (Object & @Tainted MyInterface) i;
        // :: error: (assignment.type.incompatible)
        @Untainted Object o4 = (@Tainted Object & MyInterface) i;
    }
}
