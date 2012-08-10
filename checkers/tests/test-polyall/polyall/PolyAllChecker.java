package polyall;

import polyall.quals.*;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.util.MultiGraphQualifierHierarchy;


@TypeQualifiers( {H1Top.class, H1S1.class, H1S2.class, H1Bot.class,
    H2Top.class, H2S1.class, H2S2.class, H2Bot.class,
    H1Poly.class, H2Poly.class, PolyAll.class} )
public class PolyAllChecker extends BaseTypeChecker {

    @Override
    protected MultiGraphQualifierHierarchy.MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }
}
