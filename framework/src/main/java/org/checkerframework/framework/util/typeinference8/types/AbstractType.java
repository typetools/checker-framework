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
import java.util.Set;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeFactory;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedArrayType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedDeclaredType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedIntersectionType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedWildcardType;
import org.checkerframework.framework.type.AnnotatedTypeParameterBounds;
import org.checkerframework.framework.util.AnnotatedTypes;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.javacutil.TypesUtils;
import org.plumelib.util.IPair;

/**
 * As explained in <a
 * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-18.html#jls-18.1">section 18.1</a>,
 * the JLS Chapter on type inference use the term "type" to "include type-like syntax that contains
 * inference variables". This class represents this types. Three subclasses of this class are:
 *
 * <ul>
 *   <li>{@link ProperType}: types that do not contain inference variables
 *   <li>{@link Variable}: inference variables
 *   <li>{@link InferenceType}: type-like syntax that contain at least one inference variable
 * </ul>
 */
public abstract class AbstractType {

  /** The kind of {@link AbstractType}. */
  public enum Kind {
    /** {@link ProperType},a type that contains no inference variables* */
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

  /** Whether the annotations on this type should be ignored. */
  public final boolean ignoreAnnotations;

  /**
   * Creates an {@link AbstractType}.
   *
   * @param context the context object
   * @param ignoreAnnotations whether the annotations on this type should be ignored
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
   * Return true if this type is a proper type.
   *
   * @return true if this type is a proper type
   */
  public boolean isProper() {
    return getKind() == Kind.PROPER;
  }

  /**
   * Return true if this type is a use of an inference variable.
   *
   * @return true if this type is a use of an inference variable
   */
  public boolean isUseOfVariable() {
    return getKind() == Kind.USE_OF_VARIABLE;
  }

  /**
   * Return true if this type contains inference variables, but is not an inference variable.
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
   * @param type type mirror
   * @param ignoreAnnotations whether the annotations on this type should be ignored
   * @return the new type
   */
  public abstract AbstractType create(
      AnnotatedTypeMirror atm, TypeMirror type, boolean ignoreAnnotations);

  /**
   * Return the underlying Java type without inference variables.
   *
   * @return the underlying Java type without inference variables
   */
  public abstract TypeMirror getJavaType();

  /**
   * Return the underlying Java type without inference variables.
   *
   * @return the underlying Java type without inference variables
   */
  public abstract AnnotatedTypeMirror getAnnotatedType();

  /**
   * Return a collection of all inference variables referenced by this type.
   *
   * @return a collection of all inference variables referenced by this type
   */
  public abstract Collection<Variable> getInferenceVariables();

  /**
   * Return a new type that is the same as this one except the variables in {@code instantiations}
   * have been replaced by their instantiation.
   *
   * @return a new type that is the same as this one except the variables in {@code instantiations}
   *     have been replaced by their instantiation
   */
  public abstract AbstractType applyInstantiations();

  /**
   * Return true if this type is java.lang.Object.
   *
   * @return true if this type is java.lang.Object
   */
  public abstract boolean isObject();

  /**
   * Assuming the type is a declared type, this method returns the upper bounds of its type
   * parameters. (A type parameter of a declared type, can't refer to any type being inferred, so
   * they are proper types.)
   *
   * @return the upper bounds of the type parameter of this type
   */
  public List<ProperType> getTypeParameterBounds() {
    TypeElement typeelem = (TypeElement) ((DeclaredType) getJavaType()).asElement();
    List<ProperType> bounds = new ArrayList<>();
    List<AnnotatedTypeParameterBounds> typeVars =
        typeFactory.typeVariablesFromUse((AnnotatedDeclaredType) getAnnotatedType(), typeelem);
    Iterator<? extends TypeParameterElement> javaEle = typeelem.getTypeParameters().iterator();

    for (AnnotatedTypeParameterBounds bound : typeVars) {
      TypeVariable typeVariable = (TypeVariable) javaEle.next().asType();
      bounds.add(
          new ProperType(
              bound.getUpperBound(), typeVariable.getUpperBound(), context, ignoreAnnotations));
    }
    return bounds;
  }

  /**
   * Return a new type that is the capture of this type.
   *
   * @param context the context object
   * @return a new type that is the capture of this type
   */
  public AbstractType capture(Java8InferenceContext context) {
    AnnotatedTypeMirror capturedType =
        context.typeFactory.applyCaptureConversion(getAnnotatedType());
    return create(capturedType, capturedType.getUnderlyingType(), ignoreAnnotations);
  }

  /**
   * If {@code superType} is a super type of this type, then this method returns the super type of
   * this type that is the same class as {@code superType}. Otherwise, it returns null
   *
   * @param superType a type, need not be a super type of this type
   * @return super type of this type that is the same class as {@code superType} or null if one
   *     doesn't exist
   */
  public AbstractType asSuper(TypeMirror superType) {
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
    return create(asSuper, asSuper.getUnderlyingType(), ignoreAnnotations);
  }

  /**
   * If this {@link AbstractType} is a functional interface type, then {@code functionType} is its
   * function type. Otherwise, {@code functionType} is null. Initialized by {@link
   * #getFunctionType()}.
   */
  private IPair<AnnotatedExecutableType, ExecutableType> functionType = null;

  /**
   * If this {@link AbstractType} is a functional interface type, then its function type is
   * returned. Otherwise, returns null.
   *
   * @return this {@link AbstractType} is a functional interface type, then its function type is
   *     returned; otherwise, returns null
   */
  IPair<AnnotatedExecutableType, ExecutableType> getFunctionType() {
    if (functionType == null) {
      ExecutableElement element = TypesUtils.findFunction(getJavaType(), context.env);
      AnnotatedDeclaredType groundType =
          makeGround((AnnotatedDeclaredType) getAnnotatedType(), typeFactory);
      AnnotatedExecutableType aet =
          AnnotatedTypes.asMemberOf(context.modelTypes, typeFactory, groundType, element);
      functionType = IPair.of(aet, aet.getUnderlyingType());
    }
    return functionType;
  }

  /**
   * If this type is a functional interface, then this method returns the return type of the
   * function type of that functional interface. Otherwise, returns null.
   *
   * @return the return type of the function type of this type or null if one doesn't exist
   */
  public AbstractType getFunctionTypeReturnType() {
    if (TypesUtils.isFunctionalInterface(getJavaType(), context.env)) {
      IPair<AnnotatedExecutableType, ExecutableType> pair = getFunctionType();
      ExecutableType elementType = pair.second;
      TypeMirror returnTypeJava = elementType.getReturnType();
      if (returnTypeJava.getKind() == TypeKind.VOID) {
        return null;
      }

      AnnotatedExecutableType aet = pair.first;
      AnnotatedTypeMirror returnType = aet.getReturnType();
      if (returnType.getKind() == TypeKind.VOID) {
        return null;
      }
      return create(returnType, returnTypeJava, ignoreAnnotations);
    } else {
      return null;
    }
  }

  /**
   * If this type is a functional interface, then this method returns the parameter types of the
   * function type of that functional interface. Otherwise, it returns null.
   *
   * @return the parameter types of the function type of this type or null if no function type
   *     exists
   */
  public List<AbstractType> getFunctionTypeParameterTypes() {
    if (TypesUtils.isFunctionalInterface(getJavaType(), context.env)) {
      IPair<AnnotatedExecutableType, ExecutableType> pair = getFunctionType();
      List<? extends TypeMirror> paramsTypeMirror = pair.second.getParameterTypes();
      List<AbstractType> params = new ArrayList<>();
      Iterator<? extends TypeMirror> iter = paramsTypeMirror.iterator();
      for (AnnotatedTypeMirror param : pair.first.getParameterTypes()) {
        params.add(create(param, iter.next(), ignoreAnnotations));
      }
      return params;
    } else {
      return null;
    }
  }

  /**
   * Make {@code type} ground, which is basically changing any wildcards to their bounds. <a
   * href="https://docs.oracle.com/javase/specs/jls/se11/html/jls-15.html#jls-15.27.3">JLS section
   * 15.27.3</a>
   *
   * @param type a type to ground
   * @param typeFactory type factory
   * @return the ground type
   */
  // TODO: This method is named make ground, but is actually implements non-wildcard
  // parameterization as defined in
  // https://docs.oracle.com/javase/specs/jls/se11/html/jls-9.html#jls-9.9
  // https://docs.oracle.com/javase/specs/jls/se19/html/jls-15.html#jls-15.13.2
  static AnnotatedDeclaredType makeGround(
      AnnotatedDeclaredType type, AnnotatedTypeFactory typeFactory) {
    Element e = type.getUnderlyingType().asElement();
    AnnotatedDeclaredType decl = typeFactory.getAnnotatedType((TypeElement) e);
    Iterator<AnnotatedTypeMirror> bounds = decl.getTypeArguments().iterator();

    //    typeFactory.getTypeVarSubstitutor().substitute()
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
   * Return true if the type is a raw type.
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
   * Return a new type that is the same type as this one, but whose type arguments are {@code args}.
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
    return create(newType, newTypeJava, ignoreAnnotations);
  }

  /**
   * Whether the proper type is a parameterized class or interface type, or an inner class type of a
   * parameterized class or interface type (directly or indirectly)
   *
   * @return whether T is a parameterized type
   */
  public boolean isParameterizedType() {
    // TODO this isn't matching the JavaDoc.
    return ((Type) getJavaType()).isParameterized() || ((Type) getJavaType()).isRaw();
  }

  /**
   * Return the most specific array type that is a super type of this type or null if one doesn't
   * exist.
   *
   * @return the most specific array type that is a super type of this type or null if one doesn't
   *     exist
   */
  public AbstractType getMostSpecificArrayType() {
    if (getTypeKind() == TypeKind.ARRAY) {
      return this;
    } else if (TypesUtils.isObject(getJavaType())) {
      return null;
    } else {
      AnnotatedTypeMirror msat = mostSpecificArrayType(getAnnotatedType());
      TypeMirror typeMirror =
          TypesUtils.getMostSpecificArrayType(getJavaType(), context.modelTypes);
      if (msat != null) {
        return create(msat, typeMirror, ignoreAnnotations);
      }
      return null;
    }
  }

  /**
   * Return the most specific array type, that is the first super type of {@code type} that is not
   * an array.
   *
   * @param type annotated type mirror
   * @return the first supertype of {@code type} that is an array
   */
  private static AnnotatedTypeMirror mostSpecificArrayType(AnnotatedTypeMirror type) {
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
   * Return true if this type is a primitive array.
   *
   * @return true if this type is a primitive array
   */
  public boolean isPrimitiveArray() {
    return getJavaType().getKind() == TypeKind.ARRAY
        && ((ArrayType) getJavaType()).getComponentType().getKind().isPrimitive();
  }

  /**
   * Return assuming type is an intersection type, this method returns the bounds in this type.
   *
   * @return assuming type is an intersection type, this method returns the bounds in this type
   */
  public List<AbstractType> getIntersectionBounds() {
    List<? extends TypeMirror> boundsJava = ((IntersectionType) getJavaType()).getBounds();
    Iterator<? extends TypeMirror> iter = boundsJava.iterator();
    List<AbstractType> bounds = new ArrayList<>();
    for (AnnotatedTypeMirror bound :
        ((AnnotatedIntersectionType) getAnnotatedType()).directSupertypes()) {
      bounds.add(create(bound, iter.next(), ignoreAnnotations));
    }
    return bounds;
  }

  /**
   * Return assuming this type is a type variable, this method returns the upper bound of this type.
   *
   * @return assuming this type is a type variable, this method returns the upper bound of this type
   */
  public AbstractType getTypeVarUpperBound() {
    TypeMirror javaUpperBound = ((TypeVariable) getJavaType()).getUpperBound();
    return create(
        ((AnnotatedTypeVariable) getAnnotatedType()).getUpperBound(),
        javaUpperBound,
        ignoreAnnotations);
  }

  /**
   * Return assuming this type is a type variable that has a lower bound, this method returns the
   * lower bound of this type.
   *
   * @return assuming this type is a type variable that has a lower bound, this method returns the
   *     lower bound of this type
   */
  public AbstractType getTypeVarLowerBound() {
    TypeMirror lowerBound = ((TypeVariable) getJavaType()).getLowerBound();
    return create(
        ((AnnotatedTypeVariable) getAnnotatedType()).getLowerBound(),
        lowerBound,
        ignoreAnnotations);
  }

  /**
   * Return true if this type is a type variable with a lower bound.
   *
   * @return true if this type is a type variable with a lower bound
   */
  public boolean isLowerBoundTypeVariable() {
    return ((TypeVariable) getJavaType()).getLowerBound().getKind() != TypeKind.NULL;
  }

  /**
   * Return true if this type is a parameterized type whose has at least one wildcard as a type
   * argument.
   *
   * @return true if this type is a parameterized type whose has at least one wildcard as a type
   *     argument
   */
  public boolean isWildcardParameterizedType() {
    return TypesUtils.isWildcardParameterized(getJavaType());
  }

  /**
   * Return this type's type arguments or null if this type isn't a declared type.
   *
   * @return this type's type arguments or null this type isn't a declared type
   */
  public List<AbstractType> getTypeArguments() {
    if (getJavaType().getKind() != TypeKind.DECLARED) {
      return null;
    }
    if (((AnnotatedDeclaredType) getAnnotatedType()).isUnderlyingTypeRaw()) {
      return Collections.emptyList();
    }
    List<? extends TypeMirror> javaTypeArgs = ((DeclaredType) getJavaType()).getTypeArguments();
    Iterator<? extends TypeMirror> iter = javaTypeArgs.iterator();
    List<AbstractType> list = new ArrayList<>();
    for (AnnotatedTypeMirror typeArg :
        ((AnnotatedDeclaredType) getAnnotatedType()).getTypeArguments()) {
      list.add(create(typeArg, iter.next(), ignoreAnnotations));
    }
    return list;
  }

  /**
   * Return true if the type is an unbound wildcard.
   *
   * @return true if the type is an unbound wildcard
   */
  public boolean isUnboundWildcard() {
    return TypesUtils.hasNoExplicitBound(getJavaType());
  }

  /**
   * Return true if the type is a wildcard with an upper bound.
   *
   * @return true if the type is a wildcard with an upper bound
   */
  public boolean isUpperBoundedWildcard() {
    return TypesUtils.hasExplicitExtendsBound(getJavaType());
  }

  /**
   * Return true if the type is a wildcard with a lower bound.
   *
   * @return true if the type is a wildcard with a lower bound
   */
  public boolean isLowerBoundedWildcard() {
    return TypesUtils.hasExplicitSuperBound(getJavaType());
  }

  /**
   * Return if this type is a wildcard return its lower bound; otherwise, return null.
   *
   * @return if this type is a wildcard return its lower bound; otherwise, return null
   */
  public AbstractType getWildcardLowerBound() {
    if (getJavaType().getKind() == TypeKind.WILDCARD) {
      WildcardType wild = (WildcardType) getJavaType();
      return create(
          ((AnnotatedWildcardType) getAnnotatedType()).getSuperBound(),
          wild.getSuperBound(),
          ignoreAnnotations);
    }
    return null;
  }

  /**
   * Return if this type is a wildcard return its upper bound; otherwise, return null.
   *
   * @return if this type is a wildcard return its upper bound; otherwise, return null
   */
  public AbstractType getWildcardUpperBound() {
    if (getJavaType().getKind() == TypeKind.WILDCARD) {
      TypeMirror upperBoundJava = ((WildcardType) getJavaType()).getExtendsBound();
      if (upperBoundJava == null) {
        upperBoundJava = context.object.getJavaType();
      }
      return create(
          ((AnnotatedWildcardType) getAnnotatedType()).getExtendsBound(),
          upperBoundJava,
          ignoreAnnotations);
    } else {
      return null;
    }
  }

  /**
   * Return new type whose Java type is the erasure of this type.
   *
   * @return a new type whose Java type is the erasure of this type
   */
  public AbstractType getErased() {
    TypeMirror typeMirror = context.env.getTypeUtils().erasure(getJavaType());
    return create(getAnnotatedType().getErased(), typeMirror, ignoreAnnotations);
  }

  /**
   * Return the array component type fo this type or null if on does not exist.
   *
   * @return the array component type of this type or null if one does not exist
   */
  public final AbstractType getComponentType() {
    if (getJavaType().getKind() == TypeKind.ARRAY) {
      TypeMirror javaType = ((ArrayType) getJavaType()).getComponentType();
      return create(
          ((AnnotatedArrayType) getAnnotatedType()).getComponentType(),
          javaType,
          ignoreAnnotations);
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    AbstractType that = (AbstractType) o;
    if (ignoreAnnotations != that.ignoreAnnotations) {
      return false;
    }

    if (!context.equals(that.context)) {
      return false;
    }
    return typeFactory.equals(that.typeFactory);
  }

  @Override
  public int hashCode() {
    int result = context.hashCode();
    result = 31 * result + typeFactory.hashCode();
    return result;
  }
}
