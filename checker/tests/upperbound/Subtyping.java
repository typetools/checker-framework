import org.checkerframework.checker.upperbound.qual.*;

public class Subtyping {
    void test() {
        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength({"arr"}) int a = 1;
        //:: error: (assignment.type.incompatible)
        @LessThanLength({"arr"}) int a1 = 1;
        //:: error: (assignment.type.incompatible)
        @EqualToLength({"arr"}) int a2 = 1;

        //:: error: (assignment.type.incompatible)
        @LessThanLength({"arr"}) int b = a;
        //:: error: (assignment.type.incompatible)
        @EqualToLength({"arr"}) int c = a;
        @UpperBoundUnknown int d = a;

        //:: error: (assignment.type.incompatible)
        @EqualToLength({"arr"}) int e = a1;
        //:: error: (assignment.type.incompatible)
        @LessThanLength({"arr"}) int f = a2;

        //:: error: (assignment.type.incompatible)
        @LessThanLength({"arr2"}) int g = a;

        //:: error: (assignment.type.incompatible)
        @LessThanOrEqualToLength({"arr", "arr2", "arr3"}) int h = 2;

        @LessThanOrEqualToLength({"arr", "arr2"}) int h2 = h;
        @LessThanOrEqualToLength({"arr"}) int i = h;
        @LessThanOrEqualToLength({"arr", "arr3"}) int j = h;
    }
}
