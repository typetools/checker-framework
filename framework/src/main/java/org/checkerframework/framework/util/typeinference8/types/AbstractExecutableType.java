package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.nullness.qual.Nullable;
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
 * An inference type for a method call, a constructor invocation, or a compile-time declaration of a
 * method reference. This is a wrapper around {@link AnnotatedExecutableType} whose methods return
 * {@link AbstractType}.
 */
public abstract class AbstractExecutableType {

  /** The underlying annotated method or constructor type. */
  protected final AnnotatedExecutableType annotatedExecutableType;

  /** The underlying Java method or constructor type. */
  protected final ExecutableType executableType;

  /** The inference context. */
  protected final Java8InferenceContext context;

  /** The annotated type factory. */
  protected final AnnotatedTypeFactory typeFactory;

  /**
   * A mapping from polymorphic annotation to {@link QualifierVar}. It keeps track of which
   * annotation mirror is represented by which {@code QualifierVar}.
   */
  protected final AnnotationMirrorMap<QualifierVar> qualifierVars;

  /**
   * Fills in fields of abstract class AbstractExecutableType.
   *
   * @param annotatedExecutableType the underlying annotated method or constructor type
   * @param executableType the underlying Java method or constructor type. This must be an argument
   *     to the constructor, because it is not always equal to {@code
   *     annotatedExecutableType.getUnderlyingType()}.
   * @param invocation a method or constructor invocation
   * @param context the context
   */
  protected AbstractExecutableType(
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
   * Returns the thrown types of this.
   *
   * @param map a mapping from type variable to inference variable
   * @return the thrown types
   */
  public List<? extends AbstractType> getThrownTypes(Theta map) {
    List<AbstractType> thrown = new ArrayList<>();
    for (AnnotatedTypeMirror t : annotatedExecutableType.getThrownTypes()) {
      thrown.add(InferenceType.create(t, map, context));
    }
    return thrown;
  }

  /**
   * Returns the return type of this.
   *
   * @param map a mapping from type variable to inference variable, or null to treat no type
   *     variable as an inference variable
   * @return the return type
   */
  public abstract AbstractType getReturnType(@Nullable Theta map);

  /**
   * Returns the formal parameter types of this executable.
   *
   * <p>If this invocation uses varargs, then the vararg parameter is replaced by individual
   * parameters and the result has length {@code size}. Otherwise, {@code size} is ignored and the
   * result contains one element per declared formal parameter, plus one for the receiver if this is
   * an unbound method reference.
   *
   * @param map a mapping from type variable to inference variable, or null to treat no type
   *     variable as an inference variable
   * @param size the number of parameters to return; used to expand the vararg. It is ignored if
   *     this invocation does not use varargs.
   * @return the formal parameter types of this executable
   */
  public abstract List<AbstractType> getParameterTypes(@Nullable Theta map, int size);

  /**
   * Returns the formal parameter types of this executable, without expanding the vararg parameter.
   * Call this method only for an invocation that does not use varargs.
   *
   * @param map a mapping from type variable to inference variable, or null to treat no type
   *     variable as an inference variable
   * @return the formal parameter types of this executable
   */
  public List<AbstractType> getParameterTypes(@Nullable Theta map) {
    return getParameterTypes(map, annotatedExecutableType.getParameterTypes().size());
  }

  /**
   * Returns the formal parameter types of this executable. If {@code isVarargsCall} is true, then
   * the vararg parameter is replaced by individual parameters so that the result has length {@code
   * size}; otherwise, {@code size} is ignored.
   *
   * <p>This is a helper method for {@link #getParameterTypes(Theta, int)}.
   *
   * @param map a mapping from type variable to inference variable, or null to treat no type
   *     variable as an inference variable
   * @param size the number of parameters to return; used to expand the vararg. It is ignored if
   *     {@code isVarargsCall} is false.
   * @param firstParam an extra first parameter to add at the beginning of the returned list, or
   *     null
   * @param isVarargsCall true if this invocation uses varargs
   * @return the formal parameter types of this executable
   */
  protected final List<AbstractType> getParameterTypes(
      @Nullable Theta map,
      int size,
      @Nullable AnnotatedTypeMirror firstParam,
      boolean isVarargsCall) {
    List<AnnotatedTypeMirror> params =
        new ArrayList<>(
            annotatedExecutableType.getParameterTypes().size() + (firstParam == null ? 0 : 1));

    if (firstParam != null) {
      params.add(firstParam);
    }

    params.addAll(annotatedExecutableType.getParameterTypes());

    if (isVarargsCall) {
      AnnotatedTypeMirror eltATM =
          ((AnnotatedArrayType) params.remove(params.size() - 1)).getComponentType();
      for (int i = params.size(); i < size; i++) {
        params.add(eltATM);
      }
      // A varargs invocation passes at least as many arguments as the method has formal
      // parameters, not counting the vararg parameter itself, so the loop above never leaves
      // `params` longer than `size`.
      assert params.size() == size
          : String.format(
              "Expected %d parameters but found %d in %s", size, params.size(), executableType);
    }

    return InferenceType.create(params, map, qualifierVars, context);
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
