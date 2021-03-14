import org.checkerframework.checker.units.qual.N;
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
import org.checkerframework.checker.units.qual.mm;
import org.checkerframework.checker.units.qual.mm2;
import org.checkerframework.checker.units.qual.mm3;
import org.checkerframework.checker.units.qual.s;
import org.checkerframework.checker.units.util.UnitsTools;

public class Division {
  void d() {
    // Basic division of same units, no units constraint on x
    @m int am = 6 * UnitsTools.m, bm = 3 * UnitsTools.m;
    int x = am / bm;

    // :: error: (assignment.type.incompatible)
    @m int bad = am / bm;

    // Division removes the unit.
    // As unqualified would be a supertype, we add another multiplication
    // to make sure the result of the division is unqualified.
    @s int div = (am / UnitsTools.m) * UnitsTools.s;

    // units setup
    @m int m = 2 * UnitsTools.m;
    @mm int mm = 8 * UnitsTools.mm;
    @km int km = 4 * UnitsTools.km;
    @s int s = 3 * UnitsTools.s;
    @h int h = 5 * UnitsTools.h;
    @m2 int m2 = 25 * UnitsTools.m2;
    @km2 int km2 = 9 * UnitsTools.km2;
    @mm2 int mm2 = 16 * UnitsTools.mm2;
    @mPERs int mPERs = 20 * UnitsTools.mPERs;
    @kmPERh int kmPERh = 2 * UnitsTools.kmPERh;
    @mPERs2 int mPERs2 = 30 * UnitsTools.mPERs2;
    @m3 int m3 = 125 * UnitsTools.m3;
    @km3 int km3 = 27 * UnitsTools.km3;
    @mm3 int mm3 = 64 * UnitsTools.mm3;
    @kg int kg = 11 * UnitsTools.kg;
    @N int N = 7 * UnitsTools.N;

    // m / s = mPERs
    @mPERs int velocitym = m / s;
    // :: error: (assignment.type.incompatible)
    velocitym = m / h;

    // km / h = kmPERh
    @kmPERh int velocitykm = km / h;
    // :: error: (assignment.type.incompatible)
    velocitykm = km / s;

    // m2 / m = m
    @m int distancem = m2 / m;
    // :: error: (assignment.type.incompatible)
    distancem = m2 / km;

    // km2 / km = km
    @km int distancekm = km2 / km;
    // :: error: (assignment.type.incompatible)
    distancekm = km2 / m;

    // mm2 / mm = mm
    @mm int distancemm = mm2 / mm;
    // :: error: (assignment.type.incompatible)
    distancemm = km2 / mm;

    // m3 / m2 = m
    distancem = m3 / m2;
    // :: error: (assignment.type.incompatible)
    distancem = m3 / km2;

    // km3 / km2 = km
    distancekm = km3 / km2;
    // :: error: (assignment.type.incompatible)
    distancekm = km3 / m2;

    // mm3 / mm2 = mm
    distancemm = mm3 / mm2;
    // :: error: (assignment.type.incompatible)
    distancemm = km3 / mm2;

    // m / mPERs = s
    @s int times = m / mPERs;
    // :: error: (assignment.type.incompatible)
    times = km / mPERs;

    // km / kmPERh = h
    @h int timeh = km / kmPERh;
    // :: error: (assignment.type.incompatible)
    timeh = m / kmPERh;

    // mPERs / s = mPERs2
    @mPERs2 int accel1 = mPERs / s;
    // :: error: (assignment.type.incompatible)
    accel1 = kmPERh / s;

    // mPERs / mPERs2 = s
    @s int times2 = mPERs / mPERs2;
    // :: error: (assignment.type.incompatible)
    times2 = kmPERh / mPERs2;

    // mPERs2 = N / kg
    @mPERs2 int accel2 = N / kg;
    // :: error: (assignment.type.incompatible)
    accel2 = N / km;

    // kg = N / mPERs2
    @kg int mass = N / mPERs2;
    // :: error: (assignment.type.incompatible)
    mass = s / mPERs2;
  }

  void SpeedOfSoundTests() {
    @mPERs double speedOfSound = (340.29 * UnitsTools.m) / (UnitsTools.s);

    @s double tenSeconds = 10.0 * UnitsTools.s;
    @m double soundIn10Seconds = speedOfSound * tenSeconds;

    @m double length = 100.0 * UnitsTools.m;
    @s double soundNeedTimeForLength = length / speedOfSound;
  }
}
