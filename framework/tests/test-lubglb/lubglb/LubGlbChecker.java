package lubglb;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;

import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.qual.TypeQualifiers;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationUtils;

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
        assert AnnotationUtils.areSame(qh.leastUpperBound(D, E), C) :
            "LUB of D and E is not C!";

        // System.out.println("LUB of E and D: " + qh.leastUpperBound(E, D));
        assert AnnotationUtils.areSame(qh.leastUpperBound(E, D), C) :
            "LUB of E and D is not C!";

        // System.out.println("GLB of B and C: " + qh.greatestLowerBound(B, C));
        assert AnnotationUtils.areSame(qh.greatestLowerBound(B, C), D) :
            "GLB of B and C is not D!";

        // System.out.println("GLB of C and B: " + qh.greatestLowerBound(C, B));
        assert AnnotationUtils.areSame(qh.greatestLowerBound(C, B), D) :
            "GLB of C and B is not D!";
    }
}
