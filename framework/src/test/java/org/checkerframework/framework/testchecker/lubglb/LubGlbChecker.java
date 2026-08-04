package org.checkerframework.framework.testchecker.lubglb;

import com.sun.source.util.TreePath;
import java.util.Collections;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
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
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.QualifierTyping;
import org.checkerframework.framework.util.typeinference8.types.AbstractQualifier;
import org.checkerframework.framework.util.typeinference8.types.Qualifier;
import org.checkerframework.framework.util.typeinference8.types.QualifierVar;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorMap;
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

  /** True if {@link #runConstraintEqualityTests} has already run. It only needs to run once. */
  private boolean ranConstraintEqualityTests = false;

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

    lubAssert(D, E, C);
    lubAssert(E, D, C);

    glbAssert(B, C, D);
    glbAssert(C, B, D);

    glbAssert(POLY, B, F);
    glbAssert(POLY, F, F);
    glbAssert(POLY, A, POLY);

    lubAssert(POLY, B, A);
    lubAssert(POLY, F, POLY);
    lubAssert(POLY, A, A);
  }

  @Override
  public void typeProcess(TypeElement element, TreePath path) {
    if (!ranConstraintEqualityTests && path != null) {
      ranConstraintEqualityTests = true;
      runConstraintEqualityTests(path);
    }
    super.typeProcess(element, path);
  }

  /**
   * Tests that {@link Qualifier} implements {@code equals} and {@code hashCode} in terms of its
   * annotation, that {@link QualifierTyping} implements {@code equals} and {@code hashCode} in
   * terms of its kind and its two qualifiers, and that {@link ConstraintSet} therefore maintains
   * the invariant documented on its {@code list} field: "It does not contain constraints that are
   * equal." Throws an {@code AssertionError} if a test fails.
   *
   * <p>A {@code QualifierTyping} can only be built from {@link AbstractQualifier}s, which in turn
   * can only be built from a {@link Java8InferenceContext}, which needs a {@code TreePath}. That is
   * why these tests are here rather than in {@link #initChecker} or in an ordinary JUnit test. Only
   * this checker's qualifiers are used, as operands of the constraints under test; the type
   * hierarchy is irrelevant.
   *
   * @param path a path to the class currently being processed; any path will do
   */
  private void runConstraintEqualityTests(TreePath path) {
    AnnotatedTypeFactory factory = ((BaseTypeVisitor<?>) visitor).getTypeFactory();
    Java8InferenceContext context =
        new Java8InferenceContext(factory, path, new InvocationTypeInference(factory, path));
    AbstractQualifier top = createQualifier(A, context);
    AbstractQualifier bottom = createQualifier(F, context);
    // Each call to createQualifier returns a distinct Qualifier object, so these are equal to but
    // not identical to top and bottom. Inference creates such duplicates, so constraints must be
    // compared by the meaning of their qualifiers rather than by reference.
    AbstractQualifier equalTop = createQualifier(A, context);
    AbstractQualifier equalBottom = createQualifier(F, context);

    check(top != equalTop, "createQualifier returned the same object twice; test is vacuous");
    check(top.equals(equalTop), "Qualifiers for the same annotation are not equal");
    check(top.hashCode() == equalTop.hashCode(), "Equal Qualifiers have different hash codes");
    check(!top.equals(bottom), "Qualifiers for different annotations are equal");

    QualifierTyping subtype = new QualifierTyping(bottom, top, Kind.QUALIFIER_SUBTYPE);
    QualifierTyping sameSubtype =
        new QualifierTyping(equalBottom, equalTop, Kind.QUALIFIER_SUBTYPE);
    QualifierTyping reversedSubtype = new QualifierTyping(top, bottom, Kind.QUALIFIER_SUBTYPE);
    QualifierTyping equality = new QualifierTyping(bottom, top, Kind.QUALIFIER_EQUALITY);

    check(
        subtype.equals(sameSubtype),
        "QualifierTypings with the same kind and qualifiers are not equal");
    check(sameSubtype.equals(subtype), "QualifierTyping.equals is not symmetric");
    check(
        subtype.hashCode() == sameSubtype.hashCode(),
        "Equal QualifierTypings have different hash codes");
    check(!subtype.equals(reversedSubtype), "QualifierTypings with swapped qualifiers are equal");
    check(!subtype.equals(equality), "QualifierTypings with different kinds are equal");
    // These are Object-typed so that Error Prone's EqualsIncompatibleType check does not fire.
    Object nullObject = null;
    Object notAConstraint = "not a constraint";
    check(!subtype.equals(nullObject), "A QualifierTyping is equal to null");
    check(!subtype.equals(notAConstraint), "A QualifierTyping is equal to a String");

    ConstraintSet addSet = new ConstraintSet();
    addSet.add(subtype);
    addSet.add(sameSubtype);
    addSet.pop();
    check(addSet.isEmpty(), "ConstraintSet.add did not deduplicate equal constraints");

    ConstraintSet pushSet = new ConstraintSet();
    pushSet.push(subtype);
    pushSet.push(sameSubtype);
    pushSet.pop();
    check(pushSet.isEmpty(), "ConstraintSet.push did not deduplicate equal constraints");

    ConstraintSet addAllSet = new ConstraintSet(subtype);
    addAllSet.addAll(new ConstraintSet(sameSubtype));
    addAllSet.pop();
    check(addAllSet.isEmpty(), "ConstraintSet.addAll did not deduplicate equal constraints");

    ConstraintSet removeSet = new ConstraintSet(subtype);
    removeSet.remove(new ConstraintSet(sameSubtype));
    check(removeSet.isEmpty(), "ConstraintSet.remove did not remove an equal constraint");
  }

  /**
   * Returns a newly-created {@link AbstractQualifier} for the given annotation.
   *
   * @param anno an annotation that this checker supports
   * @param context the inference context
   * @return an {@link AbstractQualifier} for {@code anno}
   */
  private AbstractQualifier createQualifier(AnnotationMirror anno, Java8InferenceContext context) {
    return AbstractQualifier.create(
            Collections.singleton(anno), new AnnotationMirrorMap<QualifierVar>(), context)
        .iterator()
        .next();
  }

  /**
   * Throws an {@code AssertionError} with the given message if {@code condition} is false.
   *
   * @param condition the condition that must hold
   * @param message the message for the {@code AssertionError}
   */
  private void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }

  /**
   * Throws an exception if glb(arg1, arg2) != result.
   *
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @param expected the expected result
   */
  private void glbAssert(AnnotationMirror arg1, AnnotationMirror arg2, AnnotationMirror expected) {
    QualifierHierarchy qualHierarchy =
        ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();
    AnnotationMirror result = qualHierarchy.greatestLowerBoundQualifiersOnly(arg1, arg2);
    if (!AnnotationUtils.areSame(expected, result)) {
      throw new AssertionError(
          String.format("GLB of %s and %s should be %s, but is %s", arg1, arg2, expected, result));
    }
  }

  /**
   * Throws an exception if lub(arg1, arg2) != result.
   *
   * @param arg1 the first argument
   * @param arg2 the second argument
   * @param expected the expected result
   */
  private void lubAssert(AnnotationMirror arg1, AnnotationMirror arg2, AnnotationMirror expected) {
    QualifierHierarchy qualHierarchy =
        ((BaseTypeVisitor<?>) visitor).getTypeFactory().getQualifierHierarchy();
    AnnotationMirror result = qualHierarchy.leastUpperBoundQualifiersOnly(arg1, arg2);
    if (!AnnotationUtils.areSame(expected, result)) {
      throw new AssertionError(
          String.format("LUB of %s and %s should be %s, but is %s", arg1, arg2, expected, result));
    }
  }
}
