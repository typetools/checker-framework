import org.checkerframework.checker.units.qual.N;
import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.g;
import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.kN;
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
import org.checkerframework.checker.units.qual.t;
import org.checkerframework.checker.units.util.UnitsTools;

public class Multiples {
  void m() {
    // Prefix assignment tests
    // kg
    @kg int kg = 5 * UnitsTools.kg;
    @g(Prefix.kilo) int alsokg = kg;
    // :: error: (assignment.type.incompatible)
    @g(Prefix.giga) int notkg = kg;
    // :: error: (assignment.type.incompatible)
    kg = notkg;
    kg = alsokg;

    // g
    @g int g = 5 * UnitsTools.g;
    @g(Prefix.one) int alsog = g;
    // :: error: (assignment.type.incompatible)
    @g(Prefix.milli) int notg = g;
    // :: error: (assignment.type.incompatible)
    notg = g;
    g = alsog;

    // m
    @m int m = 5 * UnitsTools.m;
    @m(Prefix.one) int alsom = m;
    // :: error: (assignment.type.incompatible)
    @m(Prefix.giga) int notm = m;
    // :: error: (assignment.type.incompatible)
    m = notm;
    m = alsom;

    // km
    @km int km = 5 * UnitsTools.km;
    @m(Prefix.kilo) int alsokm = km;
    // :: error: (assignment.type.incompatible)
    @m(Prefix.giga) int notkm = km;
    // :: error: (assignment.type.incompatible)
    km = notkm;
    km = alsokm;

    // mm
    @mm int mm = 5 * UnitsTools.mm;
    @m(Prefix.milli) int alsomm = mm;
    // :: error: (assignment.type.incompatible)
    @m(Prefix.giga) int notmm = mm;
    // :: error: (assignment.type.incompatible)
    mm = notmm;
    mm = alsomm;

    // N
    @N int N = 5 * UnitsTools.N;
    @N(Prefix.one)
    int alsoN = N;
    @N(Prefix.giga)
    // :: error: (assignment.type.incompatible)
    int notN = N;
    // :: error: (assignment.type.incompatible)
    N = notN;
    N = alsoN;

    // kN
    @kN int kN = 5 * UnitsTools.kN;
    @N(Prefix.kilo)
    int alsokN = kN;
    @N(Prefix.giga)
    // :: error: (assignment.type.incompatible)
    int notkN = kN;
    // :: error: (assignment.type.incompatible)
    kN = notkN;
    kN = alsokN;

    // s
    @s int s = 5 * UnitsTools.s;

    // h
    @h int h = 5 * UnitsTools.h;

    // m * m = m2
    @m2 int area = m * m;
    // :: error: (assignment.type.incompatible)
    @km2 int areambad1 = m * m;
    // :: error: (assignment.type.incompatible)
    @mm2 int areambad2 = m * m;

    // km * km = km2
    @km2 int karea = km * km;
    // :: error: (assignment.type.incompatible)
    @m2 int areakmbad1 = km * km;
    // :: error: (assignment.type.incompatible)
    @mm2 int areakmbad2 = km * km;

    // mm * mm = mm2
    @mm2 int marea = mm * mm;
    // :: error: (assignment.type.incompatible)
    @m2 int areammbad1 = mm * mm;
    // :: error: (assignment.type.incompatible)
    @km2 int areammbad2 = mm * mm;

    // m * m2 = m3
    @m3 int volume = m * area;
    // :: error: (assignment.type.incompatible)
    @km3 int volumembad1 = m * area;
    // :: error: (assignment.type.incompatible)
    @mm3 int volumembad2 = m * area;

    // km * km2 = km3
    @km3 int kvolume = km * karea;
    // :: error: (assignment.type.incompatible)
    @m3 int volumekmbad1 = km * karea;
    // :: error: (assignment.type.incompatible)
    @mm3 int volumekmbad2 = km * karea;

    // mm * mm2 = mm3
    @mm3 int mvolume = mm * marea;
    // :: error: (assignment.type.incompatible)
    @m3 int volumemmbad1 = mm * marea;
    // :: error: (assignment.type.incompatible)
    @km3 int volumemmbad2 = mm * marea;

    // m2 * m = m3
    volume = area * m;
    // :: error: (assignment.type.incompatible)
    volumembad1 = area * m;
    // :: error: (assignment.type.incompatible)
    volumembad2 = area * m;

    // km2 * km = km3
    kvolume = karea * km;
    // :: error: (assignment.type.incompatible)
    volumekmbad1 = karea * km;
    // :: error: (assignment.type.incompatible)
    volumekmbad2 = karea * km;

    // mm2 * mm = mm3
    mvolume = marea * mm;
    // :: error: (assignment.type.incompatible)
    volumemmbad1 = marea * mm;
    // :: error: (assignment.type.incompatible)
    volumemmbad2 = marea * mm;

    // s * mPERs = m
    @mPERs int speedm = 10 * UnitsTools.mPERs;
    @m int lengthm = s * speedm;
    lengthm = speedm * s;
    // :: error: (assignment.type.incompatible)
    @km int lengthmbad1 = s * speedm;
    // :: error: (assignment.type.incompatible)
    @mm int lengthmbad2 = s * speedm;

    // s * mPERs2 = mPERs
    @mPERs2 int accelm = 20 * UnitsTools.mPERs2;
    @mPERs int speedm2 = s * accelm;
    speedm2 = accelm * s;
    // :: error: (assignment.type.incompatible)
    @kmPERh int speedm2bad1 = s * accelm;

    // h * kmPERh = km
    @kmPERh int speedkm = 30 * UnitsTools.kmPERh;
    @km int lengthkm = h * speedkm;
    lengthkm = speedkm * h;
    // :: error: (assignment.type.incompatible)
    @m int lengthkmbad1 = h * speedkm;
    // :: error: (assignment.type.incompatible)
    @mm int lengthkmbad2 = h * speedkm;

    // kg * mPERs2 = N
    @kg int mass = 40 * UnitsTools.kg;
    @mPERs2 int accel = 50 * UnitsTools.mPERs2;
    @N int force = mass * accel;

    // mPERs2 * kg = N
    @N int alsoforce = accel * mass;

    @t int massMetricTons = 50 * UnitsTools.t;
    @kN int forceKiloNewtons = massMetricTons * accel;
    forceKiloNewtons = accel * massMetricTons;

    // s * s * mPERs2 = m
    // TODO: fix checker so it is insensitive to order of operations as long as final results'
    // unit makes sense.
    // Currently due to left associativity, and the lack of an s2 annotation, this tries to
    // evaluate (s * s) * mPERs2 which causes the type assignment incompatible error.
    // :: error: (assignment.type.incompatible)
    @m int distance = s * s * accelm;
    // if we bracket for order of operations, it works fine
    distance = s * (s * accelm);
  }
}
