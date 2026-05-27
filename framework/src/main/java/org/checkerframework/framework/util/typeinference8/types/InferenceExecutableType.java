package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * An inference type for a method, constructor, or method reference. This is a wrapper around {@link
 * AnnotatedExecutableType} that returns {@link AbstractType}s for the types in the {@link
 * AnnotatedExecutableType}
 */
public abstract class InferenceExecutableType {

  /** The annotated method type. */
  protected final AnnotatedExecutableType annotatedExecutableType;

  /** The Java method type. */
  protected final ExecutableType methodType;

  /** The context. */
  protected final Java8InferenceContext context;

  /** The annotated type factory. */
  protected final AnnotatedTypeFactory typeFactory;

  /** A mapping from polymorphic annotation to {@link QualifierVar}. */
  protected final AnnotationMirrorMap<QualifierVar> qualifierVars;

  /**
   * Creates an invocation type.
   *
   * @param annotatedExecutableType annotated method type
   * @param methodType java method type
   * @param invocation a method or constructor invocation
   * @param context the context
   */
  protected InferenceExecutableType(
      AnnotatedExecutableType annotatedExecutableType,
      ExecutableType methodType,
      ExpressionTree invocation,
      Java8InferenceContext context) {
    assert annotatedExecutableType != null && methodType != null;
    this.annotatedExecutableType = annotatedExecutableType;
    this.methodType = methodType;
    this.context = context;
    this.typeFactory = context.typeFactory;

    SimpleAnnotatedTypeScanner<Void, Set<AnnotationMirror>> s =
        new SimpleAnnotatedTypeScanner<>(
            (type, polys) -> {
              for (AnnotationMirror a : type.getPrimaryAnnotations()) {
                if (typeFactory.getQualifierHierarchy().isPolymorphicQualifier(a)) {
                  polys.add(a);
                }
              }
              return null;
            });
    Set<AnnotationMirror> polys = new AnnotationMirrorSet();
    s.visit(annotatedExecutableType, polys);
    AnnotationMirrorMap<QualifierVar> qualifierVars = new AnnotationMirrorMap<>();
    for (AnnotationMirror poly : polys) {
      qualifierVars.put(poly, new QualifierVar(invocation, poly, context));
    }
    this.qualifierVars = qualifierVars;
  }

  /**
   * Returns the java method type.
   *
   * @return the java method type
   */
  public ExecutableType getJavaType() {
    return annotatedExecutableType.getUnderlyingType();
  }

  /**
   * Returns the thrown types.
   *
   * @param map a mapping from type variable to inference variable
   * @return the thrown types
   */
  public List<? extends AbstractType> getThrownTypes(Theta map) {
    List<AbstractType> thrown = new ArrayList<>();
    Iterator<? extends TypeMirror> iter = methodType.getThrownTypes().iterator();
    for (AnnotatedTypeMirror t : annotatedExecutableType.getThrownTypes()) {
      thrown.add(InferenceType.create(t, iter.next(), map, context));
    }
    return thrown;
  }

  /**
   * Returns the return type.
   *
   * @param map a mapping from type variable to inference variable
   * @return the return type
   */
  public abstract AbstractType getReturnType(Theta map);

  /**
   * Returns a list of the parameter types of {@code InferenceExecutableType} where the vararg
   * parameter has been modified to match the arguments in {@code expression}.
   *
   * @param map a mapping from type variable to inference variable
   * @param size the number of parameters to return; used to expand the vararg
   * @return a list of the parameter types of {@code InferenceExecutableType} where the vararg
   *     parameter has been modified to match the arguments in {@code expression}
   */
  public abstract List<AbstractType> getParameterTypes(Theta map, int size);

  /**
   * Returns the parameter types. (Varags are not expanded.)
   *
   * @param map a mapping from type variable to inference variable
   * @return the parameter types
   */
  public List<AbstractType> getParameterTypes(Theta map) {
    return getParameterTypes(map, annotatedExecutableType.getParameterTypes().size());
  }

  /**
   * Returns true if this method has type variables.
   *
   * @return true if this method has type variables
   */
  public boolean hasTypeVariables() {
    return !annotatedExecutableType.getTypeVariables().isEmpty();
  }

  /**
   * Returns the annotated type variables.
   *
   * @return the annotated type variables
   */
  public List<? extends AnnotatedTypeVariable> getAnnotatedTypeVariables() {
    return annotatedExecutableType.getTypeVariables();
  }

  /**
   * Returns the type variables.
   *
   * @return the type variables
   */
  public List<? extends TypeVariable> getTypeVariables() {
    return methodType.getTypeVariables();
  }

  /**
   * Returns true if this method is void.
   *
   * @return true if this method is void
   */
  public boolean isVoid() {
    return annotatedExecutableType.getReturnType().getKind() == TypeKind.VOID;
  }

  /**
   * Returns the annotated method type.
   *
   * @return the annotated method type
   */
  public AnnotatedExecutableType getAnnotatedType() {
    return annotatedExecutableType;
  }
}
