package org.checkerframework.framework.testchecker.qualifierequality;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.util.TreePath;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.framework.testchecker.qualifierequality.quals.PolyQualEq;
import org.checkerframework.framework.testchecker.qualifierequality.quals.QualEqBottom;
import org.checkerframework.framework.testchecker.qualifierequality.quals.QualEqTop;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.types.AbstractQualifier;
import org.checkerframework.framework.util.typeinference8.types.QualifierVar;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.checkerframework.javacutil.trees.TreeParser;

/**
 * A checker that tests the {@code equals} and {@code hashCode} methods of the {@link
 * AbstractQualifier} subclasses {@code Qualifier} and {@link QualifierVar}, which type argument
 * inference uses.
 *
 * <p>Its type hierarchy is:
 *
 * <pre>
 *   @QualEqTop (default)
 *        |
 *   @QualEqBottom
 * </pre>
 *
 * <p>The tests are performed in {@link #typeProcess}, which throws an {@code AssertionError} if a
 * test fails. The tests need a {@link Java8InferenceContext}, which needs a {@code TreePath}, so
 * they cannot be performed in {@code initChecker} as {@code LubGlbChecker}'s tests are.
 */
public class QualifierEqualityChecker extends BaseTypeChecker {

  /** True if the tests have already been run. They only need to be run once. */
  private boolean ranTests = false;

  /** Creates a QualifierEqualityChecker. */
  public QualifierEqualityChecker() {}

  @Override
  public void typeProcess(TypeElement element, TreePath path) {
    if (!ranTests && path != null) {
      ranTests = true;
      runTests(path);
    }
    super.typeProcess(element, path);
  }

  /**
   * Runs all the tests, throwing an {@code AssertionError} if one fails.
   *
   * @param path a path to the class currently being processed; any path will do
   */
  private void runTests(TreePath path) {
    AnnotatedTypeFactory factory = ((BaseTypeVisitor<?>) visitor).getTypeFactory();
    Java8InferenceContext context =
        new Java8InferenceContext(factory, path, new InvocationTypeInference(factory, path));

    Elements elements = processingEnv.getElementUtils();
    AnnotationMirror top = AnnotationBuilder.fromClass(elements, QualEqTop.class);
    AnnotationMirror bottom = AnnotationBuilder.fromClass(elements, QualEqBottom.class);
    AnnotationMirror poly = AnnotationBuilder.fromClass(elements, PolyQualEq.class);

    // `AbstractQualifier.create` allocates a fresh `Qualifier` on every call, and
    // `AbstractType.getQualifiers()` calls it on every invocation.  Therefore, two `Qualifier`s
    // that wrap the same annotation must be `equals`; otherwise the `LinkedHashSet`s of qualifier
    // bounds in `VariableBounds` and `QualifierVar` accumulate duplicates and generate redundant
    // constraints.
    AbstractQualifier top1 = createQualifier(top, context);
    AbstractQualifier top2 = createQualifier(top, context);
    assertTrue(top1.equals(top2), "two qualifiers that wrap %s are not equal", top);
    assertTrue(top2.equals(top1), "equality of two qualifiers that wrap %s is not symmetric", top);
    assertTrue(
        top1.hashCode() == top2.hashCode(),
        "two equal qualifiers that wrap %s have different hash codes %d and %d",
        top,
        top1.hashCode(),
        top2.hashCode());

    Set<AbstractQualifier> quals = new LinkedHashSet<>();
    quals.add(top1);
    quals.add(top2);
    assertTrue(quals.size() == 1, "a set of two qualifiers that wrap %s is %s", top, quals);

    // Qualifiers that wrap different annotations are not equal.
    AbstractQualifier bottom1 = createQualifier(bottom, context);
    assertTrue(!top1.equals(bottom1), "%s equals %s", top1, bottom1);
    assertTrue(!bottom1.equals(top1), "%s equals %s", bottom1, top1);
    quals.add(bottom1);
    assertTrue(quals.size() == 2, "a set of two unequal qualifiers is %s", quals);

    // A `Qualifier` is never equal to a `QualifierVar`, even when the two wrap the same
    // polymorphic annotation.
    ExpressionTree invocation = new TreeParser(processingEnv).parseTree("dummy()");
    QualifierVar polyVar = new QualifierVar(invocation, poly, context);
    AbstractQualifier polyQual = createQualifier(poly, context);
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
   * @param context the context
   * @return a newly-created {@code Qualifier} that wraps {@code anno}
   */
  private AbstractQualifier createQualifier(AnnotationMirror anno, Java8InferenceContext context) {
    Set<AbstractQualifier> created =
        AbstractQualifier.create(
            new AnnotationMirrorSet(anno), new AnnotationMirrorMap<>(), context);
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
}
