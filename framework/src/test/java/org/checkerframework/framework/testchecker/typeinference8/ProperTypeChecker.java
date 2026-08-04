package org.checkerframework.framework.testchecker.typeinference8;

import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.TreePath;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.subtyping.qual.Bottom;
import org.checkerframework.common.subtyping.qual.Unqualified;
import org.checkerframework.framework.qual.TypeUseLocation;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.NoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.DefaultQualifierKindHierarchy;
import org.checkerframework.framework.util.QualifierKindHierarchy;
import org.checkerframework.framework.util.defaults.QualifierDefaults;
import org.checkerframework.framework.util.typeinference8.InvocationTypeInference;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TreeUtils;

/**
 * A checker that verifies invariants of {@link
 * org.checkerframework.framework.util.typeinference8.types.ProperType}, which are not observable
 * through ordinary type-checking. It issues no errors of its own; it throws an {@code
 * AssertionError} if an invariant is violated.
 *
 * <p>This checker should only be used for testing the framework.
 */
public final class ProperTypeChecker extends BaseTypeChecker {

  /** Creates a ProperTypeChecker. */
  public ProperTypeChecker() {}

  @Override
  protected BaseTypeVisitor<?> createSourceVisitor() {
    return new ProperTypeVisitor(this);
  }

  @Override
  public void typeProcessingOver() {
    if (ProperTypeVisitor.numChecks == 0) {
      throw new AssertionError(
          "ProperTypeChecker checked no proper type; the test files contain no invocation of a"
              + " method whose declared return type is a type variable.");
    }
    super.typeProcessingOver();
  }
}

/** The visitor for {@link ProperTypeChecker}. */
class ProperTypeVisitor extends BaseTypeVisitor<ProperTypeAnnotatedTypeFactory> {

  /** The number of proper types on which checking has been attempted. */
  static int numChecks = 0;

  /**
   * Creates a ProperTypeVisitor.
   *
   * @param checker the checker
   */
  public ProperTypeVisitor(BaseTypeChecker checker) {
    super(checker);
  }

  @Override
  protected ProperTypeAnnotatedTypeFactory createTypeFactory() {
    return new ProperTypeAnnotatedTypeFactory(checker);
  }

  @Override
  public Void visitMethodInvocation(MethodInvocationTree tree, Void p) {
    checkProperTypeEquality(tree);
    return super.visitMethodInvocation(tree, p);
  }

  /**
   * Creates two proper types for the type of {@code tree}. Both are created from the same annotated
   * type, but from different {@code TypeMirror}s: the type of the invocation and the declared
   * return type of the invoked method, which is a type variable. The Java type of a proper type is
   * the underlying type of its annotated type, so the two proper types are the same type and
   * therefore must be equal and must have the same hash code.
   *
   * <p>{@link org.checkerframework.framework.util.typeinference8.types.CompileTimeDeclarationType}
   * and {@link AbstractType#getErased()} create proper types in just this way: the {@code
   * TypeMirror} that they pass is not the underlying type of the annotated type that they pass.
   *
   * @param tree a method invocation
   */
  private void checkProperTypeEquality(MethodInvocationTree tree) {
    ExecutableElement method = TreeUtils.elementFromUse(tree);
    TypeMirror declaredReturnType = ((ExecutableType) method.asType()).getReturnType();
    if (declaredReturnType.getKind() != TypeKind.TYPEVAR) {
      // The two TypeMirrors would be the same, so the test would be vacuous.
      return;
    }
    AnnotatedTypeMirror atm = atypeFactory.getAnnotatedType(tree);
    if (atm.getUnderlyingType().getKind() == TypeKind.TYPEVAR) {
      // The two TypeMirrors might be the same.
      return;
    }

    TreePath path = getCurrentPath();
    // The InvocationTypeInference is needed only because Java8InferenceContext requires one.
    InvocationTypeInference inference = new InvocationTypeInference(atypeFactory, path);
    Java8InferenceContext context = new Java8InferenceContext(atypeFactory, path, inference);
    // context.object is a proper type; it is used only to call AbstractType#create.
    AbstractType fromUse = context.object.create(atm, atm.getUnderlyingType(), false);
    AbstractType fromDeclaration = context.object.create(atm, declaredReturnType, false);
    // Count the check before making the assertions, so that a failed assertion is not masked by
    // the check in ProperTypeChecker#typeProcessingOver.
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

/** The type factory for {@link ProperTypeChecker}. Its type hierarchy is Bottom <: Unqualified. */
class ProperTypeAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

  /**
   * Creates a ProperTypeAnnotatedTypeFactory.
   *
   * @param checker the checker
   */
  @SuppressWarnings("this-escape")
  public ProperTypeAnnotatedTypeFactory(BaseTypeChecker checker) {
    super(checker);
    this.postInit();
  }

  @Override
  protected void addCheckedCodeDefaults(QualifierDefaults defs) {
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(elements, Bottom.class), TypeUseLocation.LOWER_BOUND);
    defs.addCheckedCodeDefault(
        AnnotationBuilder.fromClass(elements, Unqualified.class), TypeUseLocation.OTHERWISE);
  }

  @Override
  protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
    return new HashSet<>(Arrays.asList(Unqualified.class, Bottom.class));
  }

  @Override
  protected QualifierHierarchy createQualifierHierarchy() {
    return new NoElementQualifierHierarchy(getSupportedTypeQualifiers(), elements, this) {
      @Override
      protected QualifierKindHierarchy createQualifierKindHierarchy(
          Collection<Class<? extends Annotation>> qualifierClasses) {
        return new DefaultQualifierKindHierarchy(qualifierClasses, Bottom.class);
      }
    };
  }
}
