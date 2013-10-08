package polyall;

import checkers.basetype.BaseTypeChecker;
import checkers.quals.PolyAll;
import checkers.quals.TypeQualifiers;
import checkers.types.BasicAnnotatedTypeFactory;
import checkers.types.QualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy;
import checkers.util.MultiGraphQualifierHierarchy.MultiGraphFactory;

import polyall.quals.H1Bot;
import polyall.quals.H1Poly;
import polyall.quals.H1S1;
import polyall.quals.H1S2;
import polyall.quals.H1Top;
import polyall.quals.H2Bot;
import polyall.quals.H2Poly;
import polyall.quals.H2S1;
import polyall.quals.H2S2;
import polyall.quals.H2Top;


@TypeQualifiers( {H1Top.class, H1S1.class, H1S2.class, H1Bot.class,
    H2Top.class, H2S1.class, H2S2.class, H2Bot.class,
    H1Poly.class, H2Poly.class, PolyAll.class} )
public class PolyAllChecker extends BaseTypeChecker {
}

class PolyAllAnnotatedTypeFactory extends BasicAnnotatedTypeFactory {

    public PolyAllAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);
    }

    @Override
    protected MultiGraphFactory createQualifierHierarchyFactory() {
        return new MultiGraphQualifierHierarchy.MultiGraphFactory(this);
    }

    @Override
    public QualifierHierarchy createQualifierHierarchy(MultiGraphFactory factory) {
        return new MultiGraphQualifierHierarchy(factory);
    }
}
