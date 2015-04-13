import org.checkerframework.checker.units.qual.*;
import org.checkerframework.checker.units.*;

public class BasicUnits {

    void demo() {
        //:: error: (assignment.type.incompatible)
        @m int merr = 5;

        @m int m = 5 * UnitsTools.m;
        @s int s = 9 * UnitsTools.s;

        //:: error: (assignment.type.incompatible)
        @km int kmerr = 10;
        @km int km = 10 * UnitsTools.km;

        // this is allowed, unqualified is a supertype of all units
        int bad = m / s;

        @mPERs int good = m / s;

        //:: error: (assignment.type.incompatible)
        @mPERs int b1 = s / m;

        //:: error: (assignment.type.incompatible)
        @mPERs int b2 = m * s;

        @mPERs2 int goodaccel = m / s / s;

        //:: error: (assignment.type.incompatible)
        @mPERs2 int badaccel1 = s / m / s;

        //:: error: (assignment.type.incompatible)
        @mPERs2 int badaccel2 = s / s / m;

        //:: error: (assignment.type.incompatible)
        @mPERs2 int badaccel3 = s * s / m;

        //:: error: (assignment.type.incompatible)
        @mPERs2 int badaccel4 = m * s * s;

        @Area int ae = m * m;
        @m2 int gae = m * m;

        //:: error: (assignment.type.incompatible)
        @Area int bae = m * m * m;

        //:: error: (assignment.type.incompatible)
        @km2 int bae1 = m * m;

        @km int kilometers = 10 * UnitsTools.km;
        @h int hours = UnitsTools.h;
        @kmPERh int speed = kilometers / hours;

        // Addition/substraction only accepts another @kmPERh value
        //:: error: (assignment.type.incompatible)
        speed = speed + 5;
        //:: error: (compound.assignment.type.incompatible)
        speed += 5;

        speed += speed;
        speed = (speed += speed);

        // Multiplication/division with an unqualified type is allowed
        speed = kilometers / hours * 2;
        speed /= 2;

        speed = (speed /= 2);
    }
}