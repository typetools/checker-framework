import checkers.units.UnitsTools;
import checkers.units.quals.*;

public class Multiples {
    void m() {
        @kg int kg = 5 * UnitsTools.kg;
        @g(Prefix.kilo) int alsokg = kg;

        //:: error: (assignment.type.incompatible)
        @g(Prefix.giga) int notkg = kg;

        //:: error: (assignment.type.incompatible)
        kg = notkg;

        kg = alsokg;

        @g int g1 = 5 * UnitsTools.g;
        @g(Prefix.one) int g2 = g1;
        g1 = g2;
    }
}
