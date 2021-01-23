import org.checkerframework.checker.units.qual.Prefix;
import org.checkerframework.checker.units.qual.g;
import org.checkerframework.checker.units.qual.h;
import org.checkerframework.checker.units.qual.kg;
import org.checkerframework.checker.units.qual.km;
import org.checkerframework.checker.units.qual.km2;
import org.checkerframework.checker.units.qual.kmPERh;
import org.checkerframework.checker.units.qual.m;
import org.checkerframework.checker.units.qual.m2;
import org.checkerframework.checker.units.qual.mPERs;
import org.checkerframework.checker.units.qual.mPERs2;
import org.checkerframework.checker.units.qual.mm;
import org.checkerframework.checker.units.qual.mm2;
import org.checkerframework.checker.units.qual.s;
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
