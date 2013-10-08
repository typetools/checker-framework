package lubglb;

import checkers.basetype.BaseTypeChecker;
import checkers.basetype.BaseTypeVisitor;
import checkers.quals.TypeQualifiers;
import checkers.types.QualifierHierarchy;

import javacutils.AnnotationUtils;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import lubglb.quals.A;
import lubglb.quals.B;
import lubglb.quals.C;
import lubglb.quals.D;
import lubglb.quals.E;
import lubglb.quals.F;

@TypeQualifiers( {A.class, B.class, C.class, D.class, E.class, F.class} )
public class LubGlbChecker extends BaseTypeChecker {

    private AnnotationMirror B, C, D, E; // A and F not needed

    @Override
    public void initChecker() {
        super.initChecker();

        Elements elements = processingEnv.getElementUtils();

        // A = AnnotationUtils.fromClass(elements, A.class);
        B = AnnotationUtils.fromClass(elements, B.class);
        C = AnnotationUtils.fromClass(elements, C.class);
        D = AnnotationUtils.fromClass(elements, D.class);
        E = AnnotationUtils.fromClass(elements, E.class);
        // F = AnnotationUtils.fromClass(elements, F.class);

        QualifierHierarchy qh = ((BaseTypeVisitor<?>)visitor).getTypeFactory().getQualifierHierarchy();

        // System.out.println("LUB of D and E: " + qh.leastUpperBound(D, E));
        assert qh.leastUpperBound(D, E).equals(C) :
            "LUB of D and E is not C!";

        // System.out.println("LUB of E and D: " + qh.leastUpperBound(E, D));
        assert qh.leastUpperBound(E, D).equals(C) :
            "LUB of E and D is not C!";

        // System.out.println("GLB of B and C: " + qh.greatestLowerBound(B, C));
        assert qh.greatestLowerBound(B, C).equals(D) :
            "GLB of B and C is not D!";

        // System.out.println("GLB of C and B: " + qh.greatestLowerBound(C, B));
        assert qh.greatestLowerBound(C, B).equals(D) :
            "GLB of C and B is not D!";
    }
}
