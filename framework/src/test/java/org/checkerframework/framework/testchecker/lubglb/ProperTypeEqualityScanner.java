package org.checkerframework.framework.testchecker.lubglb;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreeScanner;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.ProperType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TreeUtils;

/**
 * Verifies invariants of {@link ProperType}, which are not observable through ordinary
 * type-checking. It issues no errors of its own; it throws an {@code AssertionError} if an
 * invariant is violated.
 *
 * <p>{@link LubGlbChecker} runs this scanner over each class that it processes. The invariants hold
 * for every Java program, so this scanner needs no test input of its own; {@code
 * framework/tests/lubglb/ProperTypeEquality.java} exists only to exercise many kinds of
 * invocations.
 */
public class ProperTypeEqualityScanner extends TreeScanner<Void, Void> {

  /** The type factory of the checker that runs this scanner. */
  private final AnnotatedTypeFactory atypeFactory;

  /** The number of proper types on which checking has been attempted. */
  private int numChecks = 0;

  /**
   * A context, or null if none has been created yet. Only {@link Java8InferenceContext#object} is
   * used, so the tree path that the context was created for does not matter and one context is
   * reused for the whole compilation.
   */
  private @Nullable Java8InferenceContext context = null;

  /**
   * Creates a ProperTypeEqualityScanner.
   *
   * @param atypeFactory the type factory of the checker that runs this scanner
   */
  public ProperTypeEqualityScanner(AnnotatedTypeFactory atypeFactory) {
    this.atypeFactory = atypeFactory;
  }

  /**
   * Checks every method invocation and class instantiation under {@code path}.
   *
   * @param path a path to a class
   */
  public void checkClass(TreePath path) {
    if (context == null) {
      // The InvocationTypeInference is needed only because Java8InferenceContext requires one.
      InvocationTypeInference inference = new InvocationTypeInference(atypeFactory, path);
      context = new Java8InferenceContext(atypeFactory, path, inference);
    }
    scan(path.getLeaf(), null);
  }

  /**
   * Returns the number of proper types on which checking has been attempted.
   *
   * @return the number of proper types on which checking has been attempted
   */
  public int getNumChecks() {
    return numChecks;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    ExecutableElement method = TreeUtils.elementFromUse(tree);
    TypeMirror declaredReturnType = ((ExecutableType) method.asType()).getReturnType();
    // Check only when the declared return type is a type variable; otherwise the declared return
    // type and the type of the invocation are the same type and the check would be vacuous.
    if (declaredReturnType.getKind() == TypeKind.TYPEVAR) {
      checkProperTypeEquality(tree, declaredReturnType);
    }
    return super.visitMethodInvocation(tree, p);
  }

  @Override
  public Void visitNewClass(NewClassTree tree, Void p) {
    ExecutableElement constructor = TreeUtils.elementFromUse(tree);
    TypeElement clazz = (TypeElement) constructor.getEnclosingElement();
    // Check only when the class is generic; otherwise the class's declared type and the type of
    // the expression are the same type and the check would be vacuous.
    if (!clazz.getTypeParameters().isEmpty()) {
      checkProperTypeEquality(tree, clazz.asType());
    }
    return super.visitNewClass(tree, p);
  }

  /**
   * Creates two proper types for the type of {@code tree}. Both are created from the same annotated
   * type, but from different {@code TypeMirror}s: the type of {@code tree} and {@code
   * declaredType}, which mentions type variables that the type of {@code tree} does not. The Java
   * type of a proper type is the underlying type of its annotated type, so the two proper types are
   * the same type and therefore must be equal and must have the same hash code.
   *
   * <p>{@link org.checkerframework.framework.util.typeinference8.types.CompileTimeDeclarationType}
   * and {@link AbstractType#getErased()} create proper types in just this way: the {@code
   * TypeMirror} that they pass is not the underlying type of the annotated type that they pass.
   *
   * @param tree a method invocation or a class instantiation
   * @param declaredType the declared type corresponding to the type of {@code tree}
   */
  private void checkProperTypeEquality(ExpressionTree tree, TypeMirror declaredType) {
    AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(tree);
    @SuppressWarnings("TypeEquals") // Checking for exact object; the check would be vacuous.
    boolean sameTypeMirror = declaredType == atm.getUnderlyingType();
    if (sameTypeMirror) {
      return;
    }

    Java8InferenceContext localContext = context;
    // localContext.object is a proper type; it is used only to call AbstractType#create.
    AbstractType fromUse = localContext.object.create(atm, atm.getUnderlyingType(), false);
    AbstractType fromDeclaration = localContext.object.create(atm, declaredType, false);
    // Count the check before making the assertions, so that a failed assertion is not masked by
    // the check in LubGlbChecker#typeProcessingOver.
    numChecks++;

    if (!atypeFactory
        .getProcessingEnv()
        .getTypeUtils()
        .isSameType(fromUse.getJavaType(), fromDeclaration.getJavaType())) {
      throw new AssertionError(
          String.format(
              "For %s, the Java types of the proper types differ: %s and %s",
              tree, fromUse.getJavaType(), fromDeclaration.getJavaType()));
    }
    if (!fromUse.equals(fromDeclaration)) {
      throw new AssertionError(
          String.format(
              "For %s, proper types %s and %s have the same Java type %s, but are not equal",
              tree, fromUse, fromDeclaration, fromUse.getJavaType()));
    }
    if (fromUse.hashCode() != fromDeclaration.hashCode()) {
      throw new AssertionError(
          String.format(
              "For %s, proper types %s and %s are equal, but their hash codes differ: %d and %d",
              tree, fromUse, fromDeclaration, fromUse.hashCode(), fromDeclaration.hashCode()));
    }
  }
}
