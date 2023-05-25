package org.checkerframework.framework.testchecker.lubglb;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbA;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbB;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbC;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbD;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbE;
import org.checkerframework.framework.testchecker.lubglb.quals.LubglbF;
import org.checkerframework.framework.testchecker.lubglb.quals.PolyLubglb;
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

    A = AnnotationBuilder.fromClass(elements, LubglbA.class);
    B = AnnotationBuilder.fromClass(elements, LubglbB.class);
    C = AnnotationBuilder.fromClass(elements, LubglbC.class);
    D = AnnotationBuilder.fromClass(elements, LubglbD.class);
    E = AnnotationBuilder.fromClass(elements, LubglbE.class);
    F = AnnotationBuilder.fromClass(elements, LubglbF.class);
    POLY = AnnotationBuilder.fromClass(elements, PolyLubglb.class);

    QualifierHierarchy qh = ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();

    // System.out.println("LUB of D and E: " + qh.leastUpperBound(D, E));
    assertAnnosSame(qh.leastUpperBound(D, E), C, "LUB of D and E is not C!");

    // System.out.println("LUB of E and D: " + qh.leastUpperBound(E, D));
    assertAnnosSame(qh.leastUpperBound(E, D), C, "LUB of E and D is not C!");

    // System.out.println("GLB of B and C: " + qh.greatestLowerBound(B, C));
    assertAnnosSame(qh.greatestLowerBound(B, C), D, "GLB of B and C is not D!");

    // System.out.println("GLB of C and B: " + qh.greatestLowerBound(C, B));
    assertAnnosSame(qh.greatestLowerBound(C, B), D, "GLB of C and B is not D!");

    assertAnnosSame(qh.greatestLowerBound(POLY, B), F, "GLB of POLY and B is not F!");
    assertAnnosSame(qh.greatestLowerBound(POLY, F), F, "GLB of POLY and F is not F!");
    assertAnnosSame(qh.greatestLowerBound(POLY, A), POLY, "GLB of POLY and A is not POLY!");

    assertAnnosSame(qh.leastUpperBound(POLY, B), A, "LUB of POLY and B is not A!");
    assertAnnosSame(qh.leastUpperBound(POLY, F), POLY, "LUB of POLY and F is not POLY!");
    assertAnnosSame(qh.leastUpperBound(POLY, A), A, "LUB of POLY and A is not A!");
  }

  // This short name reduces line wrapping above.
  private void assertAnnosSame(AnnotationMirror anno1, AnnotationMirror anno2, String message) {
    assert AnnotationUtils.areSame(anno1, anno2) : message;
  }
}
