import org.checkerframework.checker.units.qual.Area;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.Volume;
import org.checkerframework.checker.units.qual.degrees;
import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.km;
import org.checkerframework.checker.units.qual.km2;
import org.checkerframework.checker.units.qual.km3;
import org.checkerframework.checker.units.qual.kmPERh;
import org.checkerframework.checker.units.qual.m;
import org.checkerframework.checker.units.qual.m2;
import org.checkerframework.checker.units.qual.m3;
import org.checkerframework.checker.units.qual.mPERs;
import org.checkerframework.checker.units.qual.mPERs2;
import org.checkerframework.checker.units.qual.radians;
import org.checkerframework.checker.units.qual.s;
import org.checkerframework.checker.units.util.UnitsTools;

public class BasicUnits {

  void demo() {
    // :: error: (assignment.type.incompatible)
    @m int merr = 5;

    @m int m = 5 * UnitsTools.m;
    @s int s = 9 * UnitsTools.s;

    // :: error: (assignment.type.incompatible)
    @km int kmerr = 10;
    @km int km = 10 * UnitsTools.km;

    // this is allowed, unqualified is a supertype of all units
    int bad = m / s;

    @mPERs int good = m / s;

    // :: error: (assignment.type.incompatible)
    @mPERs int b1 = s / m;

    // :: error: (assignment.type.incompatible)
    @mPERs int b2 = m * s;

    @mPERs2 int goodaccel = m / s / s;

    // :: error: (assignment.type.incompatible)
    @mPERs2 int badaccel1 = s / m / s;

    // :: error: (assignment.type.incompatible)
    @mPERs2 int badaccel2 = s / s / m;

    // :: error: (assignment.type.incompatible)
    @mPERs2 int badaccel3 = s * s / m;

    // :: error: (assignment.type.incompatible)
    @mPERs2 int badaccel4 = m * s * s;

    @Area int ae = m * m;
    @m2 int gae = m * m;

    // :: error: (assignment.type.incompatible)
    @Area int bae = m * m * m;

    // :: error: (assignment.type.incompatible)
    @km2 int bae1 = m * m;

    @Volume int vol = m * m * m;
    @m3 int gvol = m * m * m;

    // :: error: (assignment.type.incompatible)
    @Volume int bvol = m * m * m * m;

    // :: error: (assignment.type.incompatible)
    @km3 int bvol1 = m * m * m;

    @radians double rad = 20.0d * UnitsTools.rad;
    @degrees double deg = 30.0d * UnitsTools.deg;

    @degrees double rToD1 = UnitsTools.toDegrees(rad);
    // :: error: (argument.type.incompatible)
    @degrees double rToD2 = UnitsTools.toDegrees(deg);
    // :: error: (assignment.type.incompatible)
    @radians double rToD3 = UnitsTools.toDegrees(rad);

    @radians double dToR1 = UnitsTools.toRadians(deg);
    // :: error: (argument.type.incompatible)
    @radians double rToR2 = UnitsTools.toRadians(rad);
    // :: error: (assignment.type.incompatible)
    @degrees double rToR3 = UnitsTools.toRadians(deg);

    // speed conversion
    @mPERs int mPs = 30 * UnitsTools.mPERs;
    @kmPERh int kmPhr = 20 * UnitsTools.kmPERh;

    @kmPERh int kmPhrRes = (int) UnitsTools.fromMeterPerSecondToKiloMeterPerHour(mPs);
    @mPERs int mPsRes = (int) UnitsTools.fromKiloMeterPerHourToMeterPerSecond(kmPhr);

    // :: error: (assignment.type.incompatible)
    @mPERs int mPsResBad = (int) UnitsTools.fromMeterPerSecondToKiloMeterPerHour(mPs);
    // :: error: (assignment.type.incompatible)
    @kmPERh int kmPhrResBad = (int) UnitsTools.fromKiloMeterPerHourToMeterPerSecond(kmPhr);

    // speeds
    @km int kilometers = 10 * UnitsTools.km;
    @h int hours = UnitsTools.h;
    @kmPERh int speed = kilometers / hours;

    // Addition/substraction only accepts another @kmPERh value
    // :: error: (assignment.type.incompatible)
    speed = speed + 5;
    // :: error: (compound.assignment.type.incompatible)
    speed += 5;

    speed += speed;
    speed = (speed += speed);

    // Multiplication/division with an unqualified type is allowed
    speed = kilometers / hours * 2;
    speed /= 2;

    speed = (speed /= 2);
  }

  void prefixOutputTest() {
    @m int x = 5 * UnitsTools.m;
    @m(Prefix.kilo) int y = 2 * UnitsTools.km;
    @m(Prefix.one) int z = 3 * UnitsTools.m;
    @km int y2 = 3 * UnitsTools.km;

    // :: error: (assignment.type.incompatible)
    y2 = z;
    // :: error: (assignment.type.incompatible)
    y2 = x;
    // :: error: (assignment.type.incompatible)
    y = z;
    // :: error: (assignment.type.incompatible)
    y = x;

    // :: error: (assignment.type.incompatible)
    y2 = x * x;
    // :: error: (assignment.type.incompatible)
    y2 = z * z;
  }
}
