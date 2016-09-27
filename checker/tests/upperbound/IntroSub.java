import org.checkerframework.checker.upperbound.qual.*;

public class IntroSub {
    void test() {
        //:: error: (assignment.type.incompatible)
        /*@LessThanLength({"banana"})*/ int a = 3;
        //:: error: (assignment.type.incompatible)
        /*@EqualToLength({"banana"})*/ int b = a - (-1);
        //:: error: (assignment.type.incompatible)
        /*@LessThanLength({"banana"})*/ int c = a - (-1);
        /*@LessThanOrEqualToLength({"banana"})*/ int c1 = a - (-1);
        /*@LessThanLength({"banana"})*/ int d = a - 0;
        /*@LessThanLength({"banana"})*/ int e = a - 7;
        //:: error: (assignment.type.incompatible)
        /*@LessThanLength({"banana"})*/ int f = a - (-7);
        //:: error: (assignment.type.incompatible)
        /*@EqualToLength({"banana"})*/ int g = a - (-2);

        /*@LessThanLength({"banana"})*/ int h = a - g;
        /*@LessThanLength({"banana"})*/ int i = b - g;
        //:: error: (assignment.type.incompatible)
        /*@EqualToLength({"banana"})*/ int h2 = a - g;
        //:: error: (assignment.type.incompatible)
        /*@EqualToLength({"banana"})*/ int i2 = b - g;

        //:: error: (assignment.type.incompatible)
        /*@LessThanOrEqualToLength({"banana"})*/ int j = 7;
        /*@LessThanOrEqualToLength({"banana"})*/ int k = j - g;
        //:: error: (assignment.type.incompatible)
        /*@EqualToLength({"banana"})*/ int k2 = j - g;
    }
}
