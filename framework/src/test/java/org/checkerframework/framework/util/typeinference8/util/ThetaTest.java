package org.checkerframework.framework.util.typeinference8.util;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;
import java.lang.annotation.Annotation;
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
 * Tests that {@link Theta} treats two type variables that are {@link
 * TypesUtils#areSame(TypeVariable, TypeVariable)} as the same key, no matter which of its methods
 * performs the lookup.
 *
 * <p>{@link Theta} used to be a {@code LinkedHashMap} subclass that overrode only {@code
 * containsKey} and {@code get}. Every other lookup method &mdash; {@code put} among them &mdash;
 * used {@code TypeVariable.equals}, which javac's {@code Type.TypeVar} does not override and which
 * is therefore reference equality. So {@code theta.put(typeVar1, v1); theta.put(typeVar2, v2);} put
 * two entries in the map for one type variable, even when {@code typeVar1} and {@code typeVar2} are
 * the same type variable.
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
   * are not set up, which is enough for the lookup methods that this test exercises.
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
    AnnotatedTypeVariable annotatedTypeVariable = typeVariableOfGenericMethod();
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
    AnnotatedTypeVariable annotatedTypeVariable = typeVariableOfGenericMethod();
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
    AnnotatedTypeVariable annotatedTypeVariable = typeVariableOfGenericMethod();
    TypeVariable typeVariable = annotatedTypeVariable.getUnderlyingType();
    TypeMirror upperBound = typeVariable.getUpperBound();
    Assert.assertNotEquals(TypeKind.TYPEVAR, upperBound.getKind());

    Theta theta = new Theta();
    theta.put(typeVariable, new ValueOnlyVariable(annotatedTypeVariable, typeVariable, 1));

    Assert.assertNull(theta.get(upperBound));
  }

  /**
   * Returns the annotated type variable of {@code java.util.Collections.singletonList}, which is a
   * generic method, that is, one whose {@code getTypeVariables()} is non-empty.
   *
   * @return the annotated type variable of a generic method
   */
  private static AnnotatedTypeVariable typeVariableOfGenericMethod() {
    Context context = new Context();
    ProcessingEnvironment env = JavacProcessingEnvironment.instance(context);
    JavaCompiler javac = JavaCompiler.instance(context);
    // The list of modules must be initialized before entering symbols.
    javac.initModules(com.sun.tools.javac.util.List.nil());
    javac.enterDone();

    ValueChecker checker = new ValueChecker();
    checker.init(env);
    AnnotatedTypeFactory typeFactory = new AnnotatedTypeFactory(checker);

    TypeElement collections = env.getElementUtils().getTypeElement("java.util.Collections");
    for (ExecutableElement method : ElementFilter.methodsIn(collections.getEnclosedElements())) {
      if (method.getSimpleName().contentEquals("singletonList")) {
        AnnotatedExecutableType methodType =
            (AnnotatedExecutableType)
                AnnotatedTypeMirror.createType(method.asType(), typeFactory, true);
        return methodType.getTypeVariables().get(0);
      }
    }
    throw new AssertionError("java.util.Collections.singletonList not found");
  }
}
