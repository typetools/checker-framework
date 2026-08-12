package org.checkerframework.framework.util.typeinference8.util;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.util.ElementFilter;
import org.checkerframework.common.value.ValueChecker;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.javacutil.TypesUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the key equality of {@link Theta}: two type variables are the same key exactly when {@link
 * TypesUtils#areSame(TypeVariable, TypeVariable)} returns true for them, no matter which of {@code
 * Theta}'s methods performs the lookup.
 *
 * <p>This is not the same as {@code TypeVariable.equals}, which javac's {@code Type.TypeVar} does
 * not override and which is therefore reference equality.
 */
public class ThetaTest {

  /** Creates a new ThetaTest. */
  public ThetaTest() {}

  /**
   * A type variable that is {@link TypesUtils#areSame(TypeVariable, TypeVariable)} as the type
   * variable it delegates to, but is a different object that is not {@code equals} to it. Such a
   * type variable arises in inference when a type has undergone type variable substitution, or when
   * the type variable is the type of a tree created by {@link
   * org.checkerframework.javacutil.trees.TreeBuilder}.
   */
  private static class AliasTypeVariable implements TypeVariable {

    /** The type variable that this one is an alias for. */
    private final TypeVariable delegate;

    /**
     * Creates an alias for {@code delegate}.
     *
     * @param delegate the type variable that the new type variable is an alias for
     */
    AliasTypeVariable(TypeVariable delegate) {
      this.delegate = delegate;
    }

    @Override
    public Element asElement() {
      return delegate.asElement();
    }

    @Override
    public TypeMirror getUpperBound() {
      return delegate.getUpperBound();
    }

    @Override
    public TypeMirror getLowerBound() {
      return delegate.getLowerBound();
    }

    @Override
    public TypeKind getKind() {
      return delegate.getKind();
    }

    @Override
    public <R, P> R accept(TypeVisitor<R, P> visitor, P data) {
      return visitor.visitTypeVariable(this, data);
    }

    @Override
    public List<? extends AnnotationMirror> getAnnotationMirrors() {
      return delegate.getAnnotationMirrors();
    }

    @Override
    public <A extends Annotation> A getAnnotation(Class<A> annotationType) {
      return delegate.getAnnotation(annotationType);
    }

    @Override
    public <A extends Annotation> A[] getAnnotationsByType(Class<A> annotationType) {
      return delegate.getAnnotationsByType(annotationType);
    }

    @Override
    public String toString() {
      return "alias of " + delegate;
    }
  }

  /**
   * An inference variable that is used only as a value in a {@link Theta}. Its context and bounds
   * are not set up, which is enough for the lookup methods that this test exercises. In particular,
   * it never has an instantiation, because creating one requires a {@code Java8InferenceContext},
   * which in turn requires a path into compiled source code.
   */
  private static class ValueOnlyVariable extends Variable {

    /**
     * Creates an inference variable for {@code typeVariable}.
     *
     * @param typeVariable an annotated type variable
     * @param typeVariableJava the Java type variable
     * @param id a unique number for this variable
     */
    ValueOnlyVariable(AnnotatedTypeVariable typeVariable, TypeVariable typeVariableJava, int id) {
      super(typeVariable, typeVariableJava, null, null, null, id);
    }
  }

  /**
   * {@code put} replaces the entry for a type variable that is {@code areSame} as the given one,
   * rather than adding a second entry for the same type variable.
   */
  @Test
  public void putUsesAreSame() {
    AnnotatedTypeVariable annotatedTypeVariable = typeVariableOfGenericMethod("singletonList");
    TypeVariable typeVariable = annotatedTypeVariable.getUnderlyingType();
    TypeVariable alias = new AliasTypeVariable(typeVariable);
    Assert.assertTrue(TypesUtils.areSame(typeVariable, alias));

    Variable first = new ValueOnlyVariable(annotatedTypeVariable, typeVariable, 1);
    Variable second = new ValueOnlyVariable(annotatedTypeVariable, alias, 2);

    Theta theta = new Theta();
    theta.put(typeVariable, first);
    theta.put(alias, second);

    Assert.assertEquals(
        "put created a second entry for one type variable", 1, theta.values().size());
    Assert.assertSame(second, theta.get(typeVariable));
    Assert.assertSame(second, theta.get(alias));
  }

  /** {@code get} finds an entry that was made under an {@code areSame} key. */
  @Test
  public void lookupUsesAreSame() {
    AnnotatedTypeVariable annotatedTypeVariable = typeVariableOfGenericMethod("singletonList");
    TypeVariable typeVariable = annotatedTypeVariable.getUnderlyingType();
    TypeVariable alias = new AliasTypeVariable(typeVariable);

    Variable variable = new ValueOnlyVariable(annotatedTypeVariable, typeVariable, 1);

    Theta theta = new Theta();
    theta.put(alias, variable);

    Assert.assertSame(variable, theta.get(typeVariable));
    Assert.assertTrue(theta.containsValue(variable));
    Assert.assertEquals(1, theta.getTypeVariables().size());
  }

  /** {@code get} returns null for a type that is not a type variable. */
  @Test
  public void lookupOfNonTypeVariable() {
    AnnotatedTypeVariable annotatedTypeVariable = typeVariableOfGenericMethod("singletonList");
    TypeVariable typeVariable = annotatedTypeVariable.getUnderlyingType();
    TypeMirror upperBound = typeVariable.getUpperBound();
    Assert.assertNotEquals(TypeKind.TYPEVAR, upperBound.getKind());

    Theta theta = new Theta();
    theta.put(typeVariable, new ValueOnlyVariable(annotatedTypeVariable, typeVariable, 1));

    Assert.assertNull(theta.get(upperBound));
  }

  /**
   * Two type variables that have the same simple name but different enclosing elements are
   * different keys. This is the guard against the key equality being too coarse.
   */
  @Test
  public void distinctTypeVariablesAreDistinctKeys() {
    AnnotatedTypeVariable singletonListT = typeVariableOfGenericMethod("singletonList");
    AnnotatedTypeVariable emptyListT = typeVariableOfGenericMethod("emptyList");
    TypeVariable typeVariable1 = singletonListT.getUnderlyingType();
    TypeVariable typeVariable2 = emptyListT.getUnderlyingType();
    // The two type variables have the same name, so only the enclosing element distinguishes them.
    Assert.assertEquals(
        typeVariable1.asElement().getSimpleName(), typeVariable2.asElement().getSimpleName());
    Assert.assertFalse(TypesUtils.areSame(typeVariable1, typeVariable2));

    Variable variable1 = new ValueOnlyVariable(singletonListT, typeVariable1, 1);
    Variable variable2 = new ValueOnlyVariable(emptyListT, typeVariable2, 2);

    Theta theta = new Theta();
    theta.put(typeVariable1, variable1);
    theta.put(typeVariable2, variable2);

    Assert.assertEquals(2, theta.values().size());
    Assert.assertEquals(2, theta.getTypeVariables().size());
    Assert.assertSame(variable1, theta.get(typeVariable1));
    Assert.assertSame(variable2, theta.get(typeVariable2));
  }

  /**
   * {@code getNotInstantiated} and {@code getTypeVariables} return the type variables that were
   * put, in the order in which they were put. No variable here has an instantiation, so the two
   * methods return the same type variables.
   */
  @Test
  public void typeVariablesAreReturnedInInsertionOrder() {
    AnnotatedTypeVariable singletonListT = typeVariableOfGenericMethod("singletonList");
    AnnotatedTypeVariable emptyListT = typeVariableOfGenericMethod("emptyList");
    TypeVariable typeVariable1 = singletonListT.getUnderlyingType();
    TypeVariable typeVariable2 = emptyListT.getUnderlyingType();

    Theta theta = new Theta();
    theta.put(typeVariable1, new ValueOnlyVariable(singletonListT, typeVariable1, 1));
    theta.put(typeVariable2, new ValueOnlyVariable(emptyListT, typeVariable2, 2));

    List<TypeVariable> expected = Arrays.asList(typeVariable1, typeVariable2);
    Assert.assertEquals(expected, new ArrayList<>(theta.getTypeVariables()));
    Assert.assertEquals(expected, new ArrayList<>(theta.getNotInstantiated()));
    // getTypeVariables caches its result; a second call returns the same type variables.
    Assert.assertEquals(expected, new ArrayList<>(theta.getTypeVariables()));
  }

  /**
   * The processing environment for {@link #typeFactory}. All the tests share one, because setting
   * one up is slow and the tests only read from it.
   */
  private static final ProcessingEnvironment env;

  /** Creates the annotated types that the tests use as keys. */
  private static final AnnotatedTypeFactory typeFactory;

  static {
    Context context = new Context();
    env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    // Any concrete checker would do; ValueChecker is one that the framework tests already depend
    // on.
    ValueChecker checker = new ValueChecker();
    checker.init(env);
    typeFactory = new AnnotatedTypeFactory(checker);
  }

  /**
   * Returns the first annotated type variable of the given method of {@code java.util.Collections}.
   *
   * @param methodName the simple name of a generic method of {@code java.util.Collections}, that
   *     is, one whose {@code getTypeVariables()} is non-empty
   * @return the first annotated type variable of the given method
   */
  private static AnnotatedTypeVariable typeVariableOfGenericMethod(String methodName) {
    TypeElement collections = env.getElementUtils().getTypeElement("java.util.Collections");
    for (ExecutableElement method : ElementFilter.methodsIn(collections.getEnclosedElements())) {
      if (method.getSimpleName().contentEquals(methodName)) {
        AnnotatedExecutableType methodType =
            (AnnotatedExecutableType)
                AnnotatedTypeMirror.createType(method.asType(), typeFactory, true);
        return methodType.getTypeVariables().get(0);
      }
    }
    throw new AssertionError("java.util.Collections." + methodName + " not found");
  }
}
