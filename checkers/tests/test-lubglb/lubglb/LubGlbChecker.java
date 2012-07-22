package lubglb;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;

import lubglb.quals.*;
import checkers.basetype.BaseTypeChecker;
import checkers.quals.TypeQualifiers;
import checkers.types.QualifierHierarchy;
import checkers.util.AnnotationUtils;

@TypeQualifiers( {A.class, B.class, C.class, D.class, E.class, F.class} )
public class LubGlbChecker extends BaseTypeChecker {

    private AnnotationMirror A, B, C, D, E, F;

    @Override
    public void initChecker(ProcessingEnvironment env) {
        super.initChecker(env);

        AnnotationUtils annoFactory = AnnotationUtils.getInstance(env);
        A = annoFactory.fromClass(A.class);
        B = annoFactory.fromClass(B.class);
        C = annoFactory.fromClass(C.class);
        D = annoFactory.fromClass(D.class);
        E = annoFactory.fromClass(E.class);
        F = annoFactory.fromClass(F.class);

        QualifierHierarchy qh = this.getQualifierHierarchy();

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
