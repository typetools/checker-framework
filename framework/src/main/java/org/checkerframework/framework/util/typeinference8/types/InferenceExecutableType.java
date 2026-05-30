package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.visitor.SimpleAnnotatedTypeScanner;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.AnnotationMirrorSet;

/**
 * An inference type for a method, constructor, or method reference. This is a wrapper around {@link
 * AnnotatedExecutableType} whose methods return {@link AbstractType}.
 */
public abstract class InferenceExecutableType {

  /** The underlying annotated method or constructor type. */
  protected final AnnotatedExecutableType annotatedExecutableType;

  /** The underlying Java method or constructor type. */
  protected final ExecutableType executableType;

  /** The inference context. */
  protected final Java8InferenceContext context;

  /** The annotated type factory. */
  protected final AnnotatedTypeFactory typeFactory;

  /** A mapping from polymorphic annotation to {@link QualifierVar}. */
  protected final AnnotationMirrorMap<QualifierVar> qualifierVars;

  /**
   * Fills in fields of abstract class InferenceExecutableType.
   *
   * @param annotatedExecutableType the underlying annotated method or constructor type
   * @param executableType the underlying Java method or constructor type. This must be an argument
   *     to the constructor, because it is not always equal to {@code
   *     annotatedExecutableType.getUnderlyingType()}.
   * @param invocation a method or constructor invocation
   * @param context the context
   */
  protected InferenceExecutableType(
      AnnotatedExecutableType annotatedExecutableType,
      ExecutableType executableType,
      ExpressionTree invocation,
      Java8InferenceContext context) {
    assert annotatedExecutableType != null && executableType != null;
    this.annotatedExecutableType = annotatedExecutableType;
    this.executableType = executableType;
    this.context = context;
    this.typeFactory = context.typeFactory;

    SimpleAnnotatedTypeScanner<Void, Set<AnnotationMirror>> polymorphicQualifierCollector =
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
    polymorphicQualifierCollector.visit(annotatedExecutableType, polys);
    AnnotationMirrorMap<QualifierVar> qualifierVars = new AnnotationMirrorMap<>();
    for (AnnotationMirror poly : polys) {
      qualifierVars.put(poly, new QualifierVar(invocation, poly, context));
    }
    this.qualifierVars = qualifierVars;
  }

  /**
   * Returns the Java method or constructor type.
   *
   * @return the Java method or constructor type
   */
  public ExecutableType getJavaType() {
    return annotatedExecutableType.getUnderlyingType();
  }

  /**
   * Returns the thrown types of this.
   *
   * @param map a mapping from type variable to inference variable
   * @return the thrown types
   */
  public List<? extends AbstractType> getThrownTypes(Theta map) {
    List<AbstractType> thrown = new ArrayList<>();
    Iterator<? extends TypeMirror> iter = executableType.getThrownTypes().iterator();
    for (AnnotatedTypeMirror t : annotatedExecutableType.getThrownTypes()) {
      thrown.add(InferenceType.create(t, iter.next(), map, context));
    }
    return thrown;
  }

  /**
   * Returns the return type of this.
   *
   * @param map a mapping from type variable to inference variable
   * @return the return type
   */
  public abstract AbstractType getReturnType(Theta map);

  /**
   * Returns a list of the parameter types of {@code InferenceExecutableType} where the vararg
   * parameter has been replaced by individual parameters so the result has length {@code size}.
   *
   * @param map a mapping from type variable to inference variable
   * @param size the number of parameters to return; used to expand the vararg
   * @return a list of the parameter types of {@code InferenceExecutableType}, of length {@code
   *     size}
   */
  public abstract List<AbstractType> getParameterTypes(Theta map, int size);

  /**
   * Returns the parameter types of this. (Varags are not expanded.)
   *
   * @param map a mapping from type variable to inference variable
   * @return the parameter types
   */
  public List<AbstractType> getParameterTypes(Theta map) {
    return getParameterTypes(map, annotatedExecutableType.getParameterTypes().size());
  }

  /**
   * Returns a list of the parameter types of {@code InferenceExecutableType} where the vararg
   * parameter has been replaced by individual parameters so the result has length {@code size}.
   *
   * <p>This is a helper method for {@link #getParameterTypes(Theta, int)}.
   *
   * @param map a mapping from type variable to inference variable
   * @param size the number of parameters to return; used to expand the vararg
   * @param firstParam an extra first parameter to add at the beginning of the returned list, or
   *     null
   * @param isVarargsCall true if this invocation is uses varargs
   * @return a list of the parameter types of {@code InferenceExecutableType}, of length {@code
   *     size}
   */
  protected final List<AbstractType> getParameterTypes(
      Theta map, int size, AnnotatedTypeMirror firstParam, boolean isVarargsCall) {
    List<AnnotatedTypeMirror> params = new ArrayList<>(size);
    List<TypeMirror> paramsJava = new ArrayList<>(size);

    if (firstParam != null) {
      params.add(firstParam);
      paramsJava.add(firstParam.getUnderlyingType());
    }

    params.addAll(annotatedExecutableType.getParameterTypes());
    paramsJava.addAll(executableType.getParameterTypes());

    if (isVarargsCall) {
      AnnotatedTypeMirror eltATM =
          ((AnnotatedArrayType) params.remove(params.size() - 1)).getComponentType();
      TypeMirror eltTM = ((ArrayType) paramsJava.remove(paramsJava.size() - 1)).getComponentType();
      for (int i = params.size(); i < size; i++) {
        params.add(eltATM);
        paramsJava.add(eltTM);
      }
    }

    return InferenceType.create(params, paramsJava, map, qualifierVars, context);
  }

  /**
   * Returns true if this type has type variables.
   *
   * @return true if this type has type variables
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
   * Returns the Java type variables.
   *
   * @return the Java type variables
   */
  public List<? extends TypeVariable> getTypeVariables() {
    return executableType.getTypeVariables();
  }

  /**
   * Returns true if this method or constructor has void return type.
   *
   * @return true if this method or constructor has void return type
   */
  public boolean isVoid() {
    return annotatedExecutableType.getReturnType().getKind() == TypeKind.VOID;
  }

  /**
   * Returns the underlying annotated method or constructor type.
   *
   * @return the underlying annotated method or constructor type
   */
  public AnnotatedExecutableType getAnnotatedType() {
    return annotatedExecutableType;
  }
}
