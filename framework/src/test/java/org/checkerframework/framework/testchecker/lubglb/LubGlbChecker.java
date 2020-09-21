package org.checkerframework.framework.testchecker.lubglb;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.lubglb.quals.A;
import org.checkerframework.framework.testchecker.lubglb.quals.B;
import org.checkerframework.framework.testchecker.lubglb.quals.C;
import org.checkerframework.framework.testchecker.lubglb.quals.D;
import org.checkerframework.framework.testchecker.lubglb.quals.E;
import org.checkerframework.framework.testchecker.lubglb.quals.F;
import org.checkerframework.framework.testchecker.lubglb.quals.Poly;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationUtils;

// Type hierarchy:
//    A       <-- @DefaultQualifierInHierarchy
//   / \
//  B   C
//   \ / \
//    D   E
//     \ /
//      F

public class LubGlbChecker extends BaseTypeChecker {

    private AnnotationMirror A, B, C, D, E, F, POLY;

    @Override
    public void initChecker() {
        super.initChecker();

        Elements elements = processingEnv.getElementUtils();

        A = AnnotationBuilder.fromClass(elements, A.class);
        B = AnnotationBuilder.fromClass(elements, B.class);
        C = AnnotationBuilder.fromClass(elements, C.class);
        D = AnnotationBuilder.fromClass(elements, D.class);
        E = AnnotationBuilder.fromClass(elements, E.class);
        F = AnnotationBuilder.fromClass(elements, F.class);
        POLY = AnnotationBuilder.fromClass(elements, Poly.class);

        QualifierHierarchy qh =
                ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();

        // System.out.println("LUB of D and E: " + qh.leastUpperBound(D, E));
        assert AnnotationUtils.areSame(qh.leastUpperBound(D, E), C) : "LUB of D and E is not C!";

        // System.out.println("LUB of E and D: " + qh.leastUpperBound(E, D));
        assert AnnotationUtils.areSame(qh.leastUpperBound(E, D), C) : "LUB of E and D is not C!";

        // System.out.println("GLB of B and C: " + qh.greatestLowerBound(B, C));
        assert AnnotationUtils.areSame(qh.greatestLowerBound(B, C), D) : "GLB of B and C is not D!";

        // System.out.println("GLB of C and B: " + qh.greatestLowerBound(C, B));
        assert AnnotationUtils.areSame(qh.greatestLowerBound(C, B), D) : "GLB of C and B is not D!";

        assert AnnotationUtils.areSame(qh.greatestLowerBound(POLY, B), F)
                : "GLB of POLY and B is not F!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(POLY, F), F)
                : "GLB of POLY and F is not F!";
        assert AnnotationUtils.areSame(qh.greatestLowerBound(POLY, A), POLY)
                : "GLB of POLY and A is not POLY!";

        assert AnnotationUtils.areSame(qh.leastUpperBound(POLY, B), A)
                : "LUB of POLY and B is not A!";
        assert AnnotationUtils.areSame(qh.leastUpperBound(POLY, F), POLY)
                : "LUB of POLY and F is not POLY!";
        assert AnnotationUtils.areSame(qh.leastUpperBound(POLY, A), A)
                : "LUB of POLY and A is not A!";
    }
}
