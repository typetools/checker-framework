package org.checkerframework.framework.testchecker.constraintequality;

import com.sun.source.tree.ClassTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.Collections;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.constraintequality.qual.ConstraintEqBottom;
import org.checkerframework.framework.testchecker.constraintequality.qual.ConstraintEqTop;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.constraint.Constraint.Kind;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.QualifierTyping;
import org.checkerframework.framework.util.typeinference8.types.AbstractQualifier;
import org.checkerframework.framework.util.typeinference8.types.QualifierVar;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorMap;

/**
 * A checker that tests that {@link QualifierTyping} implements {@code equals} and {@code hashCode}
 * in terms of its kind and its two qualifiers, and that {@link ConstraintSet} therefore maintains
 * the invariant documented on its {@code list} field: "It does not contain constraints that are
 * equal."
 *
 * <p>A {@code QualifierTyping} can only be built from {@link AbstractQualifier}s, which in turn can
 * only be built from a {@link Java8InferenceContext}. That is why these tests run inside a checker
 * rather than as an ordinary JUnit test. The type hierarchy of this checker is irrelevant; only its
 * qualifiers are used, as operands of the constraints under test.
 */
public class ConstraintEqualityChecker extends BaseTypeChecker {

  /** Creates a {@code ConstraintEqualityChecker}. */
  public ConstraintEqualityChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new ConstraintEqualityVisitor(this);
  }

  /** A visitor that tests {@link QualifierTyping#equals} when it visits the first class. */
  private static class ConstraintEqualityVisitor extends BaseTypeVisitor<BaseAnnotatedTypeFactory> {

    /** True if the tests have already run. */
    private boolean testsHaveRun = false;

    /**
     * Creates a {@code ConstraintEqualityVisitor}.
     *
     * @param checker the checker
     */
    ConstraintEqualityVisitor(BaseTypeChecker checker) {
      super(checker);
    }

    @Override
    public void processClassTree(ClassTree classTree) {
      if (!testsHaveRun) {
        testsHaveRun = true;
        testQualifierTypingEquality(getCurrentPath());
      }
      super.processClassTree(classTree);
    }

    /**
     * Throws an {@code AssertionError} if {@link QualifierTyping} does not implement {@code equals}
     * and {@code hashCode}, or if {@link ConstraintSet} does not use them.
     *
     * @param path the path to the class currently being visited; used to create an inference
     *     context
     */
    private void testQualifierTypingEquality(TreePath path) {
      Java8InferenceContext context =
          new Java8InferenceContext(
              atypeFactory, path, new InvocationTypeInference(atypeFactory, path));
      AbstractQualifier top = createQualifier(ConstraintEqTop.class, context);
      AbstractQualifier bottom = createQualifier(ConstraintEqBottom.class, context);

      QualifierTyping subtype = new QualifierTyping(bottom, top, Kind.QUALIFIER_SUBTYPE);
      QualifierTyping sameSubtype = new QualifierTyping(bottom, top, Kind.QUALIFIER_SUBTYPE);
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
     * Returns an {@link AbstractQualifier} for the given annotation class.
     *
     * @param annoClass the class of an annotation that this checker supports
     * @param context the inference context
     * @return an {@link AbstractQualifier} for {@code annoClass}
     */
    private AbstractQualifier createQualifier(
        Class<? extends Annotation> annoClass, Java8InferenceContext context) {
      AnnotationMirror anno = AnnotationBuilder.fromClass(checker.getElementUtils(), annoClass);
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
  }
}
