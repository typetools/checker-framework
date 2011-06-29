import checkers.units.quals.*;
import checkers.units.*;

public class BasicUnits {

    void demo() {
        //:: error: (assignment.type.incompatible)
        @m int merr = 5;
        
        @m int m = UnitsTools.toMeter(5);
        @s int s = UnitsTools.toSecond(9);
        
        //:: error: (assignment.type.incompatible)
        @km int kmerr = 10;
        @km int km = UnitsTools.toKiloMeter(10);

        // this is allowed, unqualified is a supertype of all units
        int bad = m / s;
        
        @mPERs int good = m / s;

        //:: error: (assignment.type.incompatible)
        @mPERs int b1 = s / m;
        
        //:: error: (assignment.type.incompatible)
        @mPERs int b2 = m * s;
        
        @Area int ae = m * m;
        @m2 int gae = m * m;
        
        //:: error: (assignment.type.incompatible)
        @Area int bae = m * m * m;
        
        //:: error: (assignment.type.incompatible)
        @km2 int bae1 = m * m;
        
        @km int kilometers = UnitsTools.toKiloMeter(10);
        @h int hours = UnitsTools.toHour(1);
        @kmPERh int speed = kilometers / hours;
        
        // Addition/substraction only accepts another @kmPERh value
        //:: error: (assignment.type.incompatible)
        speed = speed + 5;
        //:: error: (compoundassign.type.incompatible)
        speed += 5;

        speed += speed;
        speed = (speed += speed);
        
        
        // Multiplication/division with an unqualified type is allowed
        speed = kilometers / hours * 2;
        speed /= 2;
        
        speed = (speed /= 2);
    }
}