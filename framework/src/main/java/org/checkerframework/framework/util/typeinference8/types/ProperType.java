package org.checkerframework.framework.util.typeinference8.types;

import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedPrimitiveType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.util.typeinference8.bound.FalseBound;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypesUtils;

/** A type that does not contain any inference variables. */
public class ProperType extends AbstractType {

  /**
   * Exception thrown when the proper type is an uninferred type argument. This should be removed
   * once Java 8 inference is actually used by the framework.
   */
  public static class CantCompute extends RuntimeException {
    private static final long serialVersionUID = 1;
  }

  private final AnnotatedTypeMirror type;
  private final TypeMirror properType;

  public ProperType(
      AnnotatedTypeMirror type, TypeMirror properType, Java8InferenceContext context) {
    super(context);
    type = verifyTypeKinds(type, properType);

    this.properType = properType;
    this.type = type;
  }

  public ProperType(ExpressionTree tree, Java8InferenceContext context) {
    super(context);
    context.getAnnotatedTypeOfProperType = true;
    AnnotatedTypeMirror type = context.typeFactory.getAnnotatedType(tree);
    context.getAnnotatedTypeOfProperType = false;

    TypeMirror properType = TreeUtils.typeOf(tree);
    this.type = verifyTypeKinds(type, properType);
    this.properType = properType;
  }

  public ProperType(VariableTree varTree, Java8InferenceContext context) {
    super(context);
    context.getAnnotatedTypeOfProperType = true;
    AnnotatedTypeMirror type = context.typeFactory.getAnnotatedType(varTree);
    context.getAnnotatedTypeOfProperType = false;
    TypeMirror properType = TreeUtils.typeOf(varTree);
    this.type = verifyTypeKinds(type, properType);
    this.properType = properType;
  }

  /** Asserts that the underlying type of {@code atm} is the same kind as {@code typeMirror} */
  private static AnnotatedTypeMirror verifyTypeKinds(
      AnnotatedTypeMirror atm, TypeMirror typeMirror) {
    assert typeMirror != null && typeMirror.getKind() != TypeKind.VOID && atm != null;
    if (atm.getKind() == TypeKind.WILDCARD) {
      AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) atm;
      if (TypesUtils.isCapturedTypeVariable(typeMirror)) {
        atm = ((AnnotatedWildcardType) atm).capture((TypeVariable) typeMirror);
      } else if (wildcardType.isUninferredTypeArgument()) {
        // TODO: Should be removed when inference is corrected
        throw new CantCompute();
      }
    }

    assert typeMirror.getKind() == atm.getKind();
    return atm;
  }

  @Override
  public Kind getKind() {
    return Kind.PROPER;
  }

  @Override
  public AbstractType create(AnnotatedTypeMirror atm, TypeMirror type) {
    return new ProperType(atm, type, context);
  }

  /**
   * If this is a primitive type, then the proper type corresponding to its wrapper is returned.
   * Otherwise, this object is return.
   *
   * @return the proper type that is the wrapper type for this type or this if no such wrapper
   *     exists
   */
  public ProperType boxType() {
    if (properType.getKind().isPrimitive()) {
      return new ProperType(
          typeFactory.getBoxedType((AnnotatedPrimitiveType) getAnnotatedType()),
          context.types.boxedClass((Type) properType).asType(),
          context);
    }
    return this;
  }

  /**
   * Is {@code this} a subtype of {@code superType}?
   *
   * @param superType super type
   * @return if {@code this} is a subtype of {@code superType}, then return {@link
   *     ConstraintSet#TRUE}; otherwise, a false bound is returned
   */
  public ReductionResult isSubType(ProperType superType) {
    TypeMirror subType = getJavaType();
    TypeMirror superJavaType = superType.getJavaType();

    if (context.types.isSubtype((Type) subType, (Type) superJavaType)) {
      AnnotatedTypeMirror superATM = superType.getAnnotatedType();
      AnnotatedTypeMirror subATM = this.getAnnotatedType();
      if (typeFactory.getTypeHierarchy().isSubtype(subATM, superATM)) {
        return ConstraintSet.TRUE;
      } else {
        return new FalseBound(true);
      }
    } else {
      return new FalseBound(false);
    }
  }

  /**
   * Is {@code this} an unchecked subtype of {@code superType}?
   *
   * @param superType super type
   * @return if {@code this} is an unchecked subtype of {@code superType}, then return {@link
   *     ConstraintSet#TRUE}; otherwise, a false bound is returned
   */
  public ReductionResult isSubTypeUnchecked(ProperType superType) {
    TypeMirror subType = getJavaType();
    TypeMirror superJavaType = superType.getJavaType();

    if (context.types.isSubtypeUnchecked((Type) subType, (Type) superJavaType)) {
      AnnotatedTypeMirror superATM = superType.getAnnotatedType();
      AnnotatedTypeMirror subATM = this.getAnnotatedType();
      if (typeFactory.getTypeHierarchy().isSubtype(subATM, superATM)) {
        return ConstraintSet.TRUE;
      } else {
        return new FalseBound(true);
      }
    } else {
      return new FalseBound(false);
    }
  }

  /**
   * Is {@code this} assignable to {@code superType}?
   *
   * @param superType super type
   * @return if {@code this} assignable to {@code superType}, then return {@link
   *     ConstraintSet#TRUE}; otherwise, a false bound is returned
   */
  public ReductionResult isAssignable(ProperType superType) {
    TypeMirror subType = getJavaType();
    TypeMirror superJavaType = superType.getJavaType();

    if (context.types.isAssignable((Type) subType, (Type) superJavaType)) {
      AnnotatedTypeMirror superATM = superType.getAnnotatedType();
      AnnotatedTypeMirror subATM = this.getAnnotatedType();
      if (typeFactory.getTypeHierarchy().isSubtype(subATM, superATM)) {
        return ConstraintSet.TRUE;
      } else {
        return new FalseBound(true);
      }
    } else {
      return new FalseBound(false);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    ProperType otherProperType = (ProperType) o;

    if (!type.equals(otherProperType.type)) {
      return false;
    }

    return properType == otherProperType.properType // faster
        || context.env.getTypeUtils().isSameType(properType, otherProperType.properType); // slower
  }

  @Override
  public int hashCode() {
    int result = properType.toString().hashCode();
    result = 31 * result + Kind.PROPER.hashCode();
    return result;
  }

  @Override
  public TypeMirror getJavaType() {
    return properType;
  }

  @Override
  public AnnotatedTypeMirror getAnnotatedType() {
    return type;
  }

  @Override
  public boolean isObject() {
    return TypesUtils.isObject(properType);
  }

  @Override
  public Collection<Variable> getInferenceVariables() {
    return Collections.emptyList();
  }

  @Override
  public AbstractType applyInstantiations(List<Variable> instantiations) {
    return this;
  }

  @Override
  public String toString() {
    return type.toString();
  }
}
