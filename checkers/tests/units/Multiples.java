import checkers.units.UnitsTools;
import checkers.units.quals.*;

public class Multiples {
    void m() {
        @kg int kg = UnitsTools.toKiloGram(5);
        @g(Prefix.kilo) int alsokg = kg;
        
        //:: error: (assignment.type.incompatible)
        @g(Prefix.giga) int notkg = kg;
        
        //:: error: (assignment.type.incompatible)
        kg = notkg;
        
        kg = alsokg;
        
        @g int g1 = UnitsTools.toGram(5);
        @g(Prefix.one) int g2 = g1;
        g1 = g2;
    }
}
