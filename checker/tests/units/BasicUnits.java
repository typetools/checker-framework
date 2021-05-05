import org.checkerframework.checker.units.qual.Area;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.Volume;
import org.checkerframework.checker.units.qual.degrees;
import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.kg;
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
import org.checkerframework.checker.units.qual.t;
import org.checkerframework.checker.units.util.UnitsTools;

public class BasicUnits {

  void demo() {
    // :: error: (assignment)
    @m int merr = 5;

    @m int m = 5 * UnitsTools.m;
    @s int s = 9 * UnitsTools.s;

    // :: error: (assignment)
    @km int kmerr = 10;
    @km int km = 10 * UnitsTools.km;

    // this is allowed, unqualified is a supertype of all units
    int bad = m / s;

    @mPERs int good = m / s;

    // :: error: (assignment)
    @mPERs int b1 = s / m;

    // :: error: (assignment)
    @mPERs int b2 = m * s;

    @mPERs2 int goodaccel = m / s / s;

    // :: error: (assignment)
    @mPERs2 int badaccel1 = s / m / s;

    // :: error: (assignment)
    @mPERs2 int badaccel2 = s / s / m;

    // :: error: (assignment)
    @mPERs2 int badaccel3 = s * s / m;

    // :: error: (assignment)
    @mPERs2 int badaccel4 = m * s * s;

    @Area int ae = m * m;
    @m2 int gae = m * m;

    // :: error: (assignment)
    @Area int bae = m * m * m;

    // :: error: (assignment)
    @km2 int bae1 = m * m;

    @Volume int vol = m * m * m;
    @m3 int gvol = m * m * m;

    // :: error: (assignment)
    @Volume int bvol = m * m * m * m;

    // :: error: (assignment)
    @km3 int bvol1 = m * m * m;

    @radians double rad = 20.0d * UnitsTools.rad;
    @degrees double deg = 30.0d * UnitsTools.deg;

    @degrees double rToD1 = UnitsTools.toDegrees(rad);
    // :: error: (argument)
    @degrees double rToD2 = UnitsTools.toDegrees(deg);
    // :: error: (assignment)
    @radians double rToD3 = UnitsTools.toDegrees(rad);

    @radians double dToR1 = UnitsTools.toRadians(deg);
    // :: error: (argument)
    @radians double rToR2 = UnitsTools.toRadians(rad);
    // :: error: (assignment)
    @degrees double rToR3 = UnitsTools.toRadians(deg);

    // speed conversion
    @mPERs int mPs = 30 * UnitsTools.mPERs;
    @kmPERh int kmPhr = 20 * UnitsTools.kmPERh;

    @kmPERh int kmPhrRes = (int) UnitsTools.fromMeterPerSecondToKiloMeterPerHour(mPs);
    @mPERs int mPsRes = (int) UnitsTools.fromKiloMeterPerHourToMeterPerSecond(kmPhr);

    // :: error: (assignment)
    @mPERs int mPsResBad = (int) UnitsTools.fromMeterPerSecondToKiloMeterPerHour(mPs);
    // :: error: (assignment)
    @kmPERh int kmPhrResBad = (int) UnitsTools.fromKiloMeterPerHourToMeterPerSecond(kmPhr);

    // speeds
    @km int kilometers = 10 * UnitsTools.km;
    @h int hours = UnitsTools.h;
    @kmPERh int speed = kilometers / hours;

    // Addition/substraction only accepts another @kmPERh value
    // :: error: (assignment)
    speed = speed + 5;
    // :: error: (compound.assignment)
    speed += 5;

    speed += speed;
    speed = (speed += speed);

    // Multiplication/division with an unqualified type is allowed
    speed = kilometers / hours * 2;
    speed /= 2;

    speed = (speed /= 2);

    @kg int kiloGrams = 1000 * UnitsTools.kg;
    @t int metricTons = UnitsTools.fromKiloGramToMetricTon(kiloGrams);
    kiloGrams = UnitsTools.fromMetricTonToKiloGram(metricTons);
  }

  void prefixOutputTest() {
    @m int x = 5 * UnitsTools.m;
    @m(Prefix.kilo) int y = 2 * UnitsTools.km;
    @m(Prefix.one) int z = 3 * UnitsTools.m;
    @km int y2 = 3 * UnitsTools.km;

    // :: error: (assignment)
    y2 = z;
    // :: error: (assignment)
    y2 = x;
    // :: error: (assignment)
    y = z;
    // :: error: (assignment)
    y = x;

    // :: error: (assignment)
    y2 = x * x;
    // :: error: (assignment)
    y2 = z * z;
  }
}
