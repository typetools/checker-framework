package org.checkerframework.framework.testchecker.lubglb;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.util.TreePath;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
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
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.QualifierTyping;
import org.checkerframework.framework.util.typeinference8.types.AbstractQualifier;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.InferenceFactory;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.types.Qualifier;
import org.checkerframework.framework.util.typeinference8.types.QualifierVar;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds;
import org.checkerframework.framework.util.typeinference8.types.VariableBounds.BoundKind;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationUtils;
import org.checkerframework.javacutil.trees.TreeParser;

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

  /** True if {@link #runQualifierEqualityTests} has already run. It only needs to run once. */
  private boolean ranQualifierEqualityTests = false;

  /** True if {@link #runConstraintEqualityTests} has already run. It only needs to run once. */
  private boolean ranConstraintEqualityTests = false;

  /** True if {@link #runAbstractTypeTests} has already run. It only needs to run once. */
  private boolean ranAbstractTypeTests = false;

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
    if (!ranQualifierEqualityTests && path != null) {
      ranQualifierEqualityTests = true;
      runQualifierEqualityTests(path);
    }
    if (!ranConstraintEqualityTests && path != null) {
      ranConstraintEqualityTests = true;
      runConstraintEqualityTests(path);
    }
    if (!ranAbstractTypeTests && path != null) {
      ranAbstractTypeTests = true;
      runAbstractTypeTests(path);
    }
    super.typeProcess(element, path);
  }

  /**
   * Tests the {@code equals} and {@code hashCode} methods of the {@link AbstractQualifier}
   * subclasses {@code Qualifier} and {@link QualifierVar}, which type argument inference uses.
   * Throws an {@code AssertionError} if a test fails.
   *
   * <p>These tests need a {@link Java8InferenceContext}, which needs a {@code TreePath}, so they
   * cannot be performed in {@link #initChecker} as the lub and glb tests are.
   *
   * @param path a path to the class currently being processed; any path will do
   */
  private void runQualifierEqualityTests(TreePath path) {
    AnnotatedTypeFactory factory = ((BaseTypeVisitor<?>) visitor).getTypeFactory();
    Java8InferenceContext context =
        new Java8InferenceContext(factory, path, new InvocationTypeInference(factory, path));

    // `AbstractQualifier.create` allocates a fresh `Qualifier` on every call, and
    // `AbstractType.getQualifiers()` calls it on every invocation.  Therefore, two `Qualifier`s
    // that wrap the same annotation must be `equals`; otherwise the `LinkedHashSet`s of qualifier
    // bounds in `VariableBounds` and `QualifierVar` accumulate duplicates and generate redundant
    // constraints.
    AbstractQualifier a1 = createQualifier(A, context);
    AbstractQualifier a2 = createQualifier(A, context);
    assertTrue(a1.equals(a2), "two qualifiers that wrap %s are not equal", A);
    assertTrue(a2.equals(a1), "equality of two qualifiers that wrap %s is not symmetric", A);
    assertTrue(
        a1.hashCode() == a2.hashCode(),
        "two equal qualifiers that wrap %s have different hash codes %d and %d",
        A,
        a1.hashCode(),
        a2.hashCode());

    Set<AbstractQualifier> quals = new LinkedHashSet<>();
    quals.add(a1);
    quals.add(a2);
    assertTrue(quals.size() == 1, "a set of two qualifiers that wrap %s is %s", A, quals);

    // Qualifiers that wrap different annotations are not equal.
    AbstractQualifier f1 = createQualifier(F, context);
    assertTrue(!a1.equals(f1), "%s equals %s", a1, f1);
    assertTrue(!f1.equals(a1), "%s equals %s", f1, a1);
    quals.add(f1);
    assertTrue(quals.size() == 2, "a set of two unequal qualifiers is %s", quals);

    // A `Qualifier` is never equal to a `QualifierVar`, even when the two wrap the same
    // polymorphic annotation.
    ExpressionTree invocation = new TreeParser(processingEnv).parseTree("dummy()");
    QualifierVar polyVar = new QualifierVar(invocation, POLY, context);
    AbstractQualifier polyQual = createQualifier(POLY, context);
    assertTrue(!polyQual.equals(polyVar), "%s equals %s", polyQual, polyVar);
    assertTrue(!polyVar.equals(polyQual), "%s equals %s", polyVar, polyQual);
    quals.add(polyQual);
    quals.add(polyVar);
    assertTrue(quals.size() == 4, "a set of four unequal qualifiers is %s", quals);
  }

  /**
   * Creates a {@code Qualifier} that wraps {@code anno}. Each call returns a distinct object.
   *
   * @param anno an annotation supported by this checker
   * @param context the inference context
   * @return a newly-created {@code Qualifier} that wraps {@code anno}
   */
  private AbstractQualifier createQualifier(AnnotationMirror anno, Java8InferenceContext context) {
    Set<AbstractQualifier> created =
        AbstractQualifier.create(
            Collections.singleton(anno), new AnnotationMirrorMap<QualifierVar>(), context);
    assertTrue(created.size() == 1, "creating a qualifier for %s produced %s", anno, created);
    return created.iterator().next();
  }

  /**
   * Throws an {@code AssertionError} if {@code condition} is false.
   *
   * @param condition the condition that is expected to hold
   * @param format a format string for the error message
   * @param args arguments to the format string
   */
  private void assertTrue(boolean condition, String format, Object... args) {
    if (!condition) {
      throw new AssertionError(String.format(format, args));
    }
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
   * Tests how {@link AbstractType} treats the two of its properties that are not part of its
   * annotated type: {@code ignoreAnnotations} and {@code qualifierVars}. Two types that differ in
   * either one are not interchangeable, so they are not {@code equals} and a set of bounds retains
   * both. Then tests that the operations that consume such a set of bounds -- {@link
   * InferenceFactory#lub} and {@link VariableBounds#hasLowerBoundDifferentParam} -- handle a set
   * that holds both. Throws an {@code AssertionError} if a test fails.
   *
   * <p>Like the other tests in this class, these tests need a {@link Java8InferenceContext}, which
   * needs a {@code TreePath}, so they cannot be ordinary JUnit tests.
   *
   * @param path a path to the class currently being processed; any path will do
   */
  private void runAbstractTypeTests(TreePath path) {
    AnnotatedTypeFactory factory = ((BaseTypeVisitor<?>) visitor).getTypeFactory();
    Java8InferenceContext context =
        new Java8InferenceContext(factory, path, new InvocationTypeInference(factory, path));
    AnnotatedTypeMirror object = context.object.getAnnotatedType();

    // Two proper types that differ only in `ignoreAnnotations` are not equal, because
    // `checkAnnotationSubtype` returns TRUE without comparing qualifiers when either type sets it.
    ProperType compared = new ProperType(object, context, false);
    ProperType ignored = new ProperType(object, context, true);
    check(!compared.equals(ignored), "types that differ in ignoreAnnotations are equal");
    check(!ignored.equals(compared), "AbstractType.equals is not symmetric");
    ProperType compared2 = new ProperType(object, context, false);
    check(compared.equals(compared2), "types that differ in nothing are not equal");
    check(compared.hashCode() == compared2.hashCode(), "equal types have different hash codes");

    // Two proper types that differ only in `qualifierVars` are not equal, because they have
    // different qualifiers, as `getQualifiers()` shows.
    ExpressionTree invocation = new TreeParser(processingEnv).parseTree("dummy()");
    AnnotationMirrorMap<QualifierVar> qualifierVars1 = new AnnotationMirrorMap<>();
    qualifierVars1.put(POLY, new QualifierVar(invocation, POLY, context));
    AnnotationMirrorMap<QualifierVar> qualifierVars2 = new AnnotationMirrorMap<>();
    qualifierVars2.put(POLY, new QualifierVar(invocation, POLY, context));
    ProperType withVar1 = new ProperType(object, qualifierVars1, context, false);
    ProperType withVar2 = new ProperType(object, qualifierVars2, context, false);
    check(!withVar1.equals(withVar2), "types with different qualifier variables are equal");
    check(!withVar1.equals(compared), "a type with a qualifier variable equals one without");
    check(!compared.equals(withVar1), "a type without a qualifier variable equals one with");
    ProperType withVar1Again = new ProperType(object, qualifierVars1, context, false);
    check(withVar1.equals(withVar1Again), "types with the same qualifier variable are not equal");
    check(
        withVar1.hashCode() == withVar1Again.hashCode(),
        "equal types with qualifier variables have different hash codes");

    Set<AbstractType> types = new LinkedHashSet<>();
    types.add(compared);
    types.add(ignored);
    types.add(withVar1);
    types.add(withVar2);
    check(types.size() == 4, "a set of four unequal types is " + types);

    // The least upper bound of a set of proper types ignores annotations only if every type in the
    // set does; otherwise, the annotations of the types that do not ignore them are meaningful.
    // The result must not depend on the iteration order of the set.
    check(
        !lubIgnoresAnnotations(context, ignored, compared),
        "lub of a mixed set whose first element ignores annotations ignores annotations");
    check(
        !lubIgnoresAnnotations(context, compared, ignored),
        "lub of a mixed set whose second element ignores annotations ignores annotations");
    check(!lubIgnoresAnnotations(context, compared, withVar1), "lub of compared types ignores");
    ProperType ignored2 = new ProperType(object, qualifierVars1, context, true);
    check(
        lubIgnoresAnnotations(context, ignored, ignored2),
        "lub of types that both ignore annotations does not ignore");

    // Two lower bounds that differ only in `ignoreAnnotations` are the same parameterization.
    AnnotatedTypeMirror list =
        factory.getAnnotatedType(processingEnv.getElementUtils().getTypeElement("java.util.List"));
    ProperType comparedList = new ProperType(list.asUse(), context, false);
    ProperType ignoredList = new ProperType(list.asUse(), context, true);
    check(comparedList.isParameterizedType(), "java.util.List is not a parameterized type");
    // `hasLowerBoundDifferentParam` reads only the bounds and the context, so the variable whose
    // bounds these are does not matter.
    VariableBounds bounds = new VariableBounds(null, context);
    Set<AbstractType> lowerBounds = bounds.bounds.get(BoundKind.LOWER);
    lowerBounds.add(comparedList);
    lowerBounds.add(ignoredList);
    check(lowerBounds.size() == 2, "a set of two unequal lower bounds is " + lowerBounds);
    check(
        !bounds.hasLowerBoundDifferentParam(),
        "two lower bounds that differ only in ignoreAnnotations are different parameterizations");
  }

  /**
   * Returns the {@code ignoreAnnotations} field of the least upper bound of the two given types.
   *
   * @param context the inference context
   * @param first the first type to lub; it is first in the set's iteration order
   * @param second the second type to lub
   * @return true if the least upper bound of the two types ignores annotations
   */
  private boolean lubIgnoresAnnotations(
      Java8InferenceContext context, ProperType first, ProperType second) {
    Set<ProperType> properTypes = new LinkedHashSet<>();
    properTypes.add(first);
    properTypes.add(second);
    check(properTypes.size() == 2, "the two types to lub are equal: " + properTypes);
    ProperType lub = context.inferenceTypeFactory.lub(properTypes);
    check(lub != null, "lub of a nonempty set is null");
    return lub.ignoreAnnotations;
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
