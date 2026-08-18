package org.checkerframework.framework.util.typeinference8.types;

import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TypesUtils;

/**
 * This class represents "types" that "include type-like syntax that contains inference variables"
 * (see <a href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-18.html#jls-18.1.1">JLS
 * section 18.1.1</a>). Three subclasses of this class are:
 *
 * <ul>
 *   <li>{@link ProperType}: types that do not contain inference variables
 *   <li>{@link Variable}: inference variables
 *   <li>{@link InferenceType}: type-like syntax that contains at least one inference variable
 * </ul>
 */
public abstract class AbstractType {

  /** The kind of {@link AbstractType}. */
  public enum Kind {
    /** {@link ProperType}, a type that contains no inference variables. */
    PROPER,
    /** {@link UseOfVariable}, a use of an inference variable. */
    USE_OF_VARIABLE,
    /**
     * {@link InferenceType}, a type that contains inference variables, but is not an inference
     * variable.
     */
    INFERENCE_TYPE
  }

  /** The context object. */
  protected final Java8InferenceContext context;

  /** The {@link AnnotatedTypeFactory}. */
  protected final AnnotatedTypeFactory typeFactory;

  /**
   * True if the annotations on this type should be ignored.
   *
   * <p>This field applies to every qualifier hierarchy at once, even when it was set because of a
   * primary annotation that appears in only some of the hierarchies. TODO: Make this per-hierarchy,
   * so that the hierarchies without such a primary annotation are still compared.
   */
  public final boolean ignoreAnnotations;

  /**
   * Creates an {@link AbstractType}.
   *
   * @param context the context object
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   */
  protected AbstractType(Java8InferenceContext context, boolean ignoreAnnotations) {
    this.context = context;
    this.typeFactory = context.typeFactory;
    this.ignoreAnnotations = ignoreAnnotations;
  }

  /**
   * Returns the kind of {@link AbstractType}.
   *
   * @return the kind of {@link AbstractType}
   */
  public abstract Kind getKind();

  /**
   * Returns true if this type is a proper type.
   *
   * @return true if this type is a proper type
   */
  public boolean isProper() {
    return getKind() == Kind.PROPER;
  }

  /**
   * Returns true if this type is a use of an inference variable.
   *
   * @return true if this type is a use of an inference variable
   */
  public boolean isUseOfVariable() {
    return getKind() == Kind.USE_OF_VARIABLE;
  }

  /**
   * Returns true if this type contains inference variables, but is not an inference variable.
   *
   * @return true if this type contains inference variables, but is not an inference variable
   */
  public boolean isInferenceType() {
    return getKind() == Kind.INFERENCE_TYPE;
  }

  /**
   * Returns the TypeKind of the underlying Java type.
   *
   * @return the TypeKind of the underlying Java type
   */
  public final TypeKind getTypeKind() {
    return getJavaType().getKind();
  }

  /**
   * Creates a type using the given types.
   *
   * @param atm annotated type mirror
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   * @return the new type
   */
  public abstract AbstractType create(AnnotatedTypeMirror atm, boolean ignoreAnnotations);

  /**
   * Creates types using the given types.
   *
   * @param atms annotated type mirrors
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   * @return the new types
   */
  public final List<AbstractType> create(
      Collection<? extends AnnotatedTypeMirror> atms, boolean ignoreAnnotations) {
    List<AbstractType> list = new ArrayList<>(atms.size());
    for (AnnotatedTypeMirror atm : atms) {
      list.add(create(atm, ignoreAnnotations));
    }
    return list;
  }

  /**
   * Returns the underlying Java type without inference variables.
   *
   * @return the underlying Java type without inference variables
   */
  public abstract TypeMirror getJavaType();

  /**
   * Returns the underlying annotated type.
   *
   * @return the underlying annotated type
   */
  public abstract AnnotatedTypeMirror getAnnotatedType();

  /**
   * Returns a collection of all inference variables referenced by this type.
   *
   * @return a collection of all inference variables referenced by this type
   */
  public abstract Collection<Variable> getInferenceVariables();

  /**
   * Returns a new type that is the same as this one except the variables in {@code instantiations}
   * have been replaced by their instantiation.
   *
   * @return a new type that is the same as this one except the variables in {@code instantiations}
   *     have been replaced by their instantiation
   */
  public abstract AbstractType applyInstantiations();

  /**
   * Returns true if this type is java.lang.Object.
   *
   * @return true if this type is java.lang.Object
   */
  public abstract boolean isObject();

  /**
   * Assuming the type is a declared type, this method returns the upper bounds of its type
   * parameters. (A type parameter of a declared type, can't refer to any type being inferred, so
   * they are proper types.)
   *
   * <p>This implementation always returns a list. The return type is {@code @Nullable} only because
   * {@link UseOfVariable#getTypeParameterBounds()} returns null.
   *
   * @return the upper bounds of the type parameters of this type
   */
  public @Nullable List<ProperType> getTypeParameterBounds() {
    TypeElement typeelem = (TypeElement) ((DeclaredType) getJavaType()).asElement();
    List<ProperType> bounds = new ArrayList<>();
    List<AnnotatedTypeParameterBounds> typeVars =
        typeFactory.typeVariablesFromUse((AnnotatedDeclaredType) getAnnotatedType(), typeelem);
    for (AnnotatedTypeParameterBounds bound : typeVars) {
      bounds.add(new ProperType(bound.getUpperBound(), context, ignoreAnnotations));
    }
    return bounds;
  }

  /**
   * Returns a new type that is the capture of this type.
   *
   * @param context the context object
   * @return a new type that is the capture of this type
   */
  public AbstractType capture(Java8InferenceContext context) {
    AnnotatedTypeMirror capturedType =
        context.typeFactory.applyCaptureConversion(getAnnotatedType());
    return create(capturedType, ignoreAnnotations);
  }

  /**
   * If {@code superType} is a super type of this type, then this method returns the super type of
   * this type that is the same class as {@code superType}. Otherwise, it returns null
   *
   * @param superType a type, need not be a super type of this type
   * @return super type of this type that is the same class as {@code superType} or null if one
   *     doesn't exist
   */
  public @Nullable AbstractType asSuper(TypeMirror superType) {
    TypeMirror typeJava = getJavaType();
    if (typeJava.getKind() == TypeKind.WILDCARD) {
      typeJava = ((WildcardType) typeJava).getExtendsBound();
    }
    TypeMirror asSuperJava = context.types.asSuper((Type) typeJava, ((Type) superType).asElement());
    if (asSuperJava == null) {
      return null;
    }

    AnnotatedTypeMirror type = getAnnotatedType();

    if (type.getKind() == TypeKind.WILDCARD) {
      type = ((AnnotatedWildcardType) type).getExtendsBound();
    }

    AnnotatedTypeMirror superAnnotatedType =
        AnnotatedTypeMirror.createType(superType, typeFactory, type.isDeclaration());
    typeFactory.initializeAtm(superAnnotatedType);
    AnnotatedTypeMirror asSuper = AnnotatedTypes.asSuper(typeFactory, type, superAnnotatedType);
    return create(asSuper, ignoreAnnotations);
  }

  /**
   * If this {@link AbstractType} is a functional interface type, then {@code functionType} is its
   * function type. Otherwise, {@code functionType} is null. Initialized by {@link
   * #getFunctionType()}.
   */
  protected AnnotatedExecutableType functionType = null;

  /**
   * Returns true if this {@link AbstractType} is a functional interface type.
   *
   * @return true if this {@link AbstractType} is a functional interface type
   */
  public boolean isFunctionalInterface() {
    return TypesUtils.isFunctionalInterface(getJavaType(), context.env);
  }

  /**
   * If this {@link AbstractType} is a functional interface type, then its function type is
   * returned. Otherwise, returns null.
   *
   * @return this {@link AbstractType} is a functional interface type, then its function type is
   *     returned; otherwise, returns null
   */
  @Nullable AnnotatedExecutableType getFunctionType() {
    if (functionType == null && TypesUtils.isFunctionalInterface(getJavaType(), context.env)) {
      ExecutableElement element = TypesUtils.findFunction(getJavaType(), context.env);
      AnnotatedDeclaredType groundType =
          makeGround((AnnotatedDeclaredType) getAnnotatedType(), typeFactory);
      functionType =
          AnnotatedTypes.asMemberOf(context.modelTypes, typeFactory, groundType, element);
    }
    return functionType;
  }

  /**
   * If this type is a functional interface whose function type has a non-void return type, then
   * this method returns that return type. Otherwise, returns null.
   *
   * <p>Note that a null result does not distinguish between the two reasons for it: this type is
   * not a functional interface, or the return type of its function type is void.
   *
   * @return the return type of the function type of this type, or null if this type is not a
   *     functional interface or its function type returns void
   */
  public @Nullable AbstractType getFunctionTypeReturnType() {
    if (isFunctionalInterface()) {
      AnnotatedExecutableType aet = getFunctionType();
      assert aet != null : "@AssumeAssertion(nullness): this is a functional interface";
      AnnotatedTypeMirror returnType = aet.getReturnType();
      if (returnType.getKind() == TypeKind.VOID || returnType.getKind() == TypeKind.NONE) {
        return null;
      }
      return create(returnType, ignoreAnnotations);
    } else {
      return null;
    }
  }

  /**
   * If this type is a functional interface, then this method returns the parameter types of the
   * function type of that functional interface. Otherwise, it returns null.
   *
   * <p>Each call returns a new list, which the caller may modify.
   *
   * @return the parameter types of the function type of this type or null if no function type
   *     exists
   */
  public @Nullable List<AbstractType> getFunctionTypeParameterTypes() {
    if (isFunctionalInterface()) {
      AnnotatedExecutableType functionType = getFunctionType();
      assert functionType != null : "@AssumeAssertion(nullness): this is a functional interface";
      List<AbstractType> params = new ArrayList<>();
      for (AnnotatedTypeMirror param : functionType.getParameterTypes()) {
        params.add(create(param, ignoreAnnotations));
      }
      return params;
    } else {
      return null;
    }
  }

  /**
   * Make {@code type} ground, which is basically changing any wildcards to their bounds. <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-15.html#jls-15.27.3">JLS section
   * 15.27.3</a>
   *
   * <p>This method implements the non-wildcard parameterization of <a
   * href="https://docs.oracle.com/javase/specs/jls/se25/html/jls-9.html#jls-9.9">JLS section
   * 9.9</a>, for an {@link AnnotatedDeclaredType}. The method {@code
   * Expression.nonWildcardParameterization} implements the same rule for an {@link AbstractType}.
   *
   * @param type a type to ground
   * @param typeFactory type factory
   * @return the ground type
   */
  // TODO: Unify this method with Expression.nonWildcardParameterization.
  static AnnotatedDeclaredType makeGround(
      AnnotatedDeclaredType type, AnnotatedTypeFactory typeFactory) {
    Element e = type.getUnderlyingType().asElement();
    AnnotatedDeclaredType decl = typeFactory.getAnnotatedType((TypeElement) e);
    Iterator<AnnotatedTypeMirror> bounds = decl.getTypeArguments().iterator();

    Map<TypeVariable, AnnotatedTypeMirror> typeVarToTypeArg = new HashMap<>();
    for (AnnotatedTypeMirror pn : type.getTypeArguments()) {
      AnnotatedTypeVariable typeVariable = (AnnotatedTypeVariable) bounds.next();
      if (pn.getKind() != TypeKind.WILDCARD) {
        typeVarToTypeArg.put(typeVariable.getUnderlyingType(), pn);
        continue;
      }
      AnnotatedWildcardType wildcardType = (AnnotatedWildcardType) pn;
      if (wildcardType.getSuperBound().getKind() == TypeKind.NULL) {
        // › If Ai is a upper-bounded wildcard ? extends Ui, then Ti = glb(Ui, Bi)
        typeVarToTypeArg.put(
            typeVariable.getUnderlyingType(),
            AnnotatedTypes.annotatedGLB(
                typeFactory, typeVariable.getUpperBound(), wildcardType.getExtendsBound()));
      } else {
        typeVarToTypeArg.put(typeVariable.getUnderlyingType(), wildcardType.getSuperBound());
      }
    }
    return (AnnotatedDeclaredType)
        typeFactory.getTypeVarSubstitutor().substitute(typeVarToTypeArg, decl.asUse());
  }

  /**
   * Returns true if the type is a raw type.
   *
   * @return true if the type is a raw type
   */
  public boolean isRaw() {
    if (getAnnotatedType().getKind() == TypeKind.DECLARED) {
      return ((AnnotatedDeclaredType) getAnnotatedType()).isUnderlyingTypeRaw();
    }
    return false;
  }

  /**
   * Returns a new type that is the same type as this one, but whose type arguments are {@code
   * args}.
   *
   * @param args a list of type arguments
   * @return a new type that is the same type as this one, but whose type arguments are {@code args}
   */
  public AbstractType replaceTypeArgs(List<AbstractType> args) {
    DeclaredType declaredType = (DeclaredType) getJavaType();
    TypeMirror[] newArgs = new TypeMirror[args.size()];
    int i = 0;
    for (AbstractType t : args) {
      newArgs[i++] = t.getJavaType();
    }
    TypeMirror newTypeJava =
        context.env.getTypeUtils().getDeclaredType((TypeElement) declaredType.asElement(), newArgs);

    AnnotatedDeclaredType newType =
        (AnnotatedDeclaredType)
            AnnotatedTypeMirror.createType(
                newTypeJava, typeFactory, getAnnotatedType().isDeclaration());
    List<AnnotatedTypeMirror> argTypes = new ArrayList<>();
    for (AbstractType arg : args) {
      argTypes.add(arg.getAnnotatedType());
    }
    newType.setTypeArguments(argTypes);
    newType.replaceAnnotations(getAnnotatedType().getPrimaryAnnotations());
    return create(newType, ignoreAnnotations);
  }

  /**
   * Returns true if this type is a parameterized class or interface type, an inner class type of a
   * parameterized class or interface type (directly or indirectly), or a raw type.
   *
   * @return true if this type is a parameterized type or a raw type
   */
  // A raw type is not a parameterized type as JLS 4.5 defines the term, so this method is more
  // permissive than the "parameterized type" of JLS 18.2.3 and 18.5.2.1.
  // TODO: Determine whether those two rules should treat raw types as parameterized types, and if
  // not, use a different predicate there.
  public boolean isParameterizedType() {
    return ((Type) getJavaType()).isParameterized() || ((Type) getJavaType()).isRaw();
  }

  /**
   * Returns the most specific array type that is a super type of this type or null if one doesn't
   * exist.
   *
   * @return the most specific array type that is a super type of this type or null if one doesn't
   *     exist
   */
  public @Nullable AbstractType getMostSpecificArrayType() {
    if (getTypeKind() == TypeKind.ARRAY) {
      return this;
    } else if (TypesUtils.isObject(getJavaType())) {
      return null;
    } else {
      AnnotatedTypeMirror msat = mostSpecificArrayType(getAnnotatedType());
      if (msat != null) {
        return create(msat, ignoreAnnotations);
      }
      return null;
    }
  }

  /**
   * Returns the most specific array type, that is the first super type of {@code type} that is an
   * array.
   *
   * @param type annotated type mirror
   * @return the first supertype of {@code type} that is an array
   */
  private static @Nullable AnnotatedTypeMirror mostSpecificArrayType(AnnotatedTypeMirror type) {
    if (type.getKind() == TypeKind.ARRAY) {
      return type;
    } else if (TypesUtils.isObject(type.getUnderlyingType())) {
      return null;
    } else {
      for (AnnotatedTypeMirror superType : type.directSupertypes()) {
        AnnotatedTypeMirror arrayType = mostSpecificArrayType(superType);
        if (arrayType != null) {
          return arrayType;
        }
      }
      return null;
    }
  }

  /**
   * Returns true if this type is a primitive array.
   *
   * @return true if this type is a primitive array
   */
  public boolean isPrimitiveArray() {
    return getJavaType().getKind() == TypeKind.ARRAY
        && ((ArrayType) getJavaType()).getComponentType().getKind().isPrimitive();
  }

  /**
   * Returns assuming type is an intersection type, this method returns the bounds in this type.
   *
   * @return assuming type is an intersection type, this method returns the bounds in this type
   */
  public List<AbstractType> getIntersectionBounds() {
    return create(getAnnotatedType().directSupertypes(), ignoreAnnotations);
  }

  /**
   * Returns assuming this type is a type variable, this method returns the upper bound of this
   * type.
   *
   * @return assuming this type is a type variable, this method returns the upper bound of this type
   */
  public AbstractType getTypeVarUpperBound() {
    return create(((AnnotatedTypeVariable) getAnnotatedType()).getUpperBound(), ignoreAnnotations);
  }

  /**
   * Returns assuming this type is a type variable that has a lower bound, this method returns the
   * lower bound of this type.
   *
   * @return assuming this type is a type variable that has a lower bound, this method returns the
   *     lower bound of this type
   */
  public AbstractType getTypeVarLowerBound() {
    return create(((AnnotatedTypeVariable) getAnnotatedType()).getLowerBound(), ignoreAnnotations);
  }

  /**
   * Returns true if this type is a type variable with a lower bound.
   *
   * @return true if this type is a type variable with a lower bound
   */
  public boolean isLowerBoundTypeVariable() {
    return ((TypeVariable) getJavaType()).getLowerBound().getKind() != TypeKind.NULL;
  }

  /**
   * Returns true if this type is a parameterized type that has at least one wildcard as a type
   * argument.
   *
   * @return true if this type is a parameterized type that has at least one wildcard as a type
   *     argument
   */
  public boolean isWildcardParameterizedType() {
    return TypesUtils.isWildcardParameterized(getJavaType());
  }

  /**
   * Returns this type's type arguments or null if this type isn't a declared type.
   *
   * @return this type's type arguments or null if this type isn't a declared type
   */
  public @Nullable List<AbstractType> getTypeArguments() {
    AnnotatedTypeMirror atm = getAnnotatedType();
    if (atm instanceof AnnotatedDeclaredType annotatedDeclaredType) {
      if (annotatedDeclaredType.isUnderlyingTypeRaw()) {
        return Collections.emptyList();
      }
      return create(annotatedDeclaredType.getTypeArguments(), ignoreAnnotations);
    } else {
      return null;
    }
  }

  /**
   * Returns true if the type is an unbound wildcard.
   *
   * @return true if the type is an unbound wildcard
   */
  public boolean isUnboundWildcard() {
    return TypesUtils.hasNoExplicitBound(getJavaType());
  }

  /**
   * Returns true if the type is a wildcard with an upper bound.
   *
   * @return true if the type is a wildcard with an upper bound
   */
  public boolean isUpperBoundedWildcard() {
    return TypesUtils.hasExplicitExtendsBound(getJavaType());
  }

  /**
   * Returns true if the type is a wildcard with a lower bound.
   *
   * @return true if the type is a wildcard with a lower bound
   */
  public boolean isLowerBoundedWildcard() {
    return TypesUtils.hasExplicitSuperBound(getJavaType());
  }

  /**
   * If this type is a wildcard, returns its lower bound; otherwise, returns null.
   *
   * @return the lower bound of this wildcard type, or null if this type is not a wildcard
   */
  public @Nullable AbstractType getWildcardLowerBound() {
    if (getJavaType().getKind() == TypeKind.WILDCARD) {
      return create(
          ((AnnotatedWildcardType) getAnnotatedType()).getSuperBound(), ignoreAnnotations);
    }
    return null;
  }

  /**
   * If this type is a wildcard, returns its upper bound; otherwise, returns null. If the wildcard
   * has no explicit upper bound, the returned type is the upper bound of the type variable to which
   * the wildcard is bound; see {@link AnnotatedWildcardType#getExtendsBound()}.
   *
   * @return the upper bound of this wildcard type, or null if this type is not a wildcard
   */
  public @Nullable AbstractType getWildcardUpperBound() {
    if (getJavaType().getKind() == TypeKind.WILDCARD) {
      return create(
          ((AnnotatedWildcardType) getAnnotatedType()).getExtendsBound(), ignoreAnnotations);
    } else {
      return null;
    }
  }

  /**
   * Returns new type whose Java type is the erasure of this type.
   *
   * @return a new type whose Java type is the erasure of this type
   */
  public AbstractType getErased() {
    return create(getAnnotatedType().getErased(), ignoreAnnotations);
  }

  /**
   * Returns the array component type of this type or null if one does not exist.
   *
   * @return the array component type of this type or null if one does not exist
   */
  public final @Nullable AbstractType getComponentType() {
    if (getJavaType().getKind() == TypeKind.ARRAY) {
      return create(
          ((AnnotatedArrayType) getAnnotatedType()).getComponentType(), ignoreAnnotations);
    } else {
      return null;
    }
  }

  /**
   * Returns the primary qualifiers on this type.
   *
   * @return the primary qualifiers on this type
   */
  public abstract Set<AbstractQualifier> getQualifiers();

  /**
   * Checks whether the annotations of {@code this} are a subtype of those of {@code superType},
   * assuming that their underlying Java types have already been found to be in the required
   * relationship. If either type is marked as having annotations that should be ignored, then the
   * annotations are not compared.
   *
   * @param superType the potential supertype
   * @return {@link ConstraintSet#TRUE} if the annotations are ignored or if the annotations of
   *     {@code this} are a subtype of those of {@code superType}; otherwise {@link
   *     ConstraintSet#TRUE_ANNO_FAIL}
   */
  protected final ConstraintSet checkAnnotationSubtype(ProperType superType) {
    if (ignoreAnnotations || superType.ignoreAnnotations) {
      return ConstraintSet.TRUE;
    }
    AnnotatedTypeMirror subATM = getAnnotatedType();
    AnnotatedTypeMirror superATM = superType.getAnnotatedType();
    if (typeFactory.getTypeHierarchy().isSubtype(subATM, superATM)) {
      return ConstraintSet.TRUE;
    } else {
      return ConstraintSet.TRUE_ANNO_FAIL;
    }
  }

  // equals and hashCode are abstract so that a subclass must implement them.  A subclass that needs
  // to compare the fields of this class can use sameInferenceProblem and inferenceProblemHashCode.
  @Override
  public abstract boolean equals(Object o);

  @Override
  public abstract int hashCode();

  /**
   * Returns true if {@code that} belongs to the same inference problem as this type and treats
   * annotations the same way as this type does. This is a helper method for {@link #equals}; it
   * says nothing about the types themselves.
   *
   * @param that another abstract type
   * @return true if {@code that} belongs to the same inference problem as this type and treats
   *     annotations the same way as this type does
   */
  protected final boolean sameInferenceProblem(AbstractType that) {
    return ignoreAnnotations == that.ignoreAnnotations
        && context.equals(that.context)
        && typeFactory.equals(that.typeFactory);
  }

  /**
   * Returns a hash code for the fields that {@link #sameInferenceProblem} compares. This is a
   * helper method for {@link #hashCode}.
   *
   * @return a hash code for the fields that {@link #sameInferenceProblem} compares
   */
  protected final int inferenceProblemHashCode() {
    return Objects.hash(ignoreAnnotations, context, typeFactory);
  }
}
