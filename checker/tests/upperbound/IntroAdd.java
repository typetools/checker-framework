import org.checkerframework.checker.upperbound.qual.*;

public class IntroAdd {
    void test() {
        //:: error: (assignment.type.incompatible)
        /*@LessThanLength({"banana"})*/ int a = 3;
        //:: error: (assignment.type.incompatible)
        /*@EqualToLength({"banana"})*/ int b = a + 1;
        //:: error: (assignment.type.incompatible)
        /*@LessThanLength({"banana"})*/ int c = a + 1;
        /*@LessThanOrEqualToLength({"banana"})*/ int c1 = a + 1;
        /*@LessThanLength({"banana"})*/ int d = a + 0;
        /*@LessThanLength({"banana"})*/ int e = a + (-7);
        //:: error: (assignment.type.incompatible)
        /*@LessThanLength({"banana"})*/ int f = a + 7;
        //:: error: (assignment.type.incompatible)
        /*@EqualToLength({"banana"})*/ int g = a + 2;
    }
}
