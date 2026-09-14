package org.checkerframework.framework.util.typeinference8.types;

import com.sun.tools.javac.code.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedExecutableType;
import org.checkerframework.framework.type.AnnotatedTypeMirror.AnnotatedTypeVariable;
import org.checkerframework.framework.util.typeinference8.constraint.ConstraintSet;
import org.checkerframework.framework.util.typeinference8.constraint.ReductionResult;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.AnnotationMirrorMap;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A type-like structure that contains at least one inference variable, but is not an inference
 * variable.
 */
public final class InferenceType extends AbstractType {

  /**
   * The AnnotatedTypeMirror. It contains type variables that are mapped to inference variables in
   * {@code map}.
   */
  private final AnnotatedTypeMirror type;

  /** A mapping of type variables to inference variables. */
  private final Theta map;

  /** A mapping from polymorphic annotation to {@link QualifierVar}. */
  private final AnnotationMirrorMap<QualifierVar> qualifierVars;

  /**
   * Creates an inference type.
   *
   * @param type the annotated type mirror
   * @param map a mapping from type variable to inference variable
   * @param qualifierVars a mapping from polymorphic annotation to {@link QualifierVar}
   * @param context the context
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   */
  private InferenceType(
      AnnotatedTypeMirror type,
      Theta map,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context,
      boolean ignoreAnnotations) {
    super(context, ignoreAnnotations);
    this.type = type.asUse();
    this.qualifierVars = qualifierVars;
    this.map = map;
  }

  @Override
  public Kind getKind() {
    return Kind.INFERENCE_TYPE;
  }

  /**
   * Creates an abstract type for the given TypeMirror. The created type is an {@link InferenceType}
   * if {@code type} contains any type variables that are mapped to inference variables as specified
   * by {@code map}. Or if {@code type} is a type variable that is mapped to an inference variable,
   * that {@link Variable} is returned. Or if {@code type} contains no type variables that are
   * mapped to an inference variable, a {@link ProperType} is returned.
   *
   * @param type the annotated type mirror
   * @param map a mapping from type variable to inference variable
   * @param context the context
   * @return the abstract type for the given TypeMirror and AnnotatedTypeMirror
   */
  public static AbstractType create(
      AnnotatedTypeMirror type, @Nullable Theta map, Java8InferenceContext context) {
    return create(type, map, AnnotationMirrorMap.emptyMap(), context, false);
  }

  /**
   * Creates an abstract type for the given TypeMirror. The created type is an {@link InferenceType}
   * if {@code type} contains any type variables that are mapped to inference variables as specified
   * by {@code map}. Or if {@code type} is a type variable that is mapped to an inference variable,
   * that {@link Variable} is returned. Or if {@code type} contains no type variables that are
   * mapped to an inference variable, a {@link ProperType} is returned.
   *
   * @param type the annotated type mirror
   * @param map a mapping from type variable to inference variable
   * @param qualifierVars a mapping from polymorphic annotation to {@link QualifierVar}
   * @param context the context
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   * @return the abstract type for the given TypeMirror and AnnotatedTypeMirror
   */
  public static AbstractType create(
      AnnotatedTypeMirror type,
      @Nullable Theta map,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context,
      boolean ignoreAnnotations) {
    assert type != null;
    if (map == null) {
      return new ProperType(type, qualifierVars, context, ignoreAnnotations);
    }

    // The kind test is of `typeMirror`, but the lookup key is from `type`.  The two can disagree,
    // because `type` and `typeMirror` come from different sources; for example, `typeMirror` may
    // have been substituted while `type` was not.  This is a use of an inference variable only if
    // both agree that this position holds a type variable.  If only `type` is a mapped type
    // variable, the fall-through below creates an InferenceType, whose `applyInstantiations`
    // substitutes for the type variable later.
    Variable variable =
        type.getKind() == TypeKind.TYPEVAR ? map.get(type.getUnderlyingType()) : null;
    if (variable != null) {
      return new UseOfVariable(
          (AnnotatedTypeVariable) type, variable, qualifierVars, context, ignoreAnnotations);
    } else if (AnnotatedContainsInferenceVariable.hasAnyTypeVariable(
        map.getTypeVariables(), type)) {
      return new InferenceType(type, map, qualifierVars, context, ignoreAnnotations);
    } else {
      return new ProperType(type, qualifierVars, context, ignoreAnnotations);
    }
  }

  /**
   * Same as {@link #create(AnnotatedTypeMirror, Theta, AnnotationMirrorMap, Java8InferenceContext,
   * boolean)}, but if {@code type} contains any type variables that are in {@code map}, but already
   * have an instantiation, they are treated as proper types.
   *
   * @param type the annotated type mirror
   * @param typeMirror the Java type
   * @param map a mapping from type variable to inference variable
   * @param qualifierVars a mapping from polymorphic annotation to {@link QualifierVar}
   * @param context the context
   * @param ignoreAnnotations true if the annotations on this type should be ignored
   * @return the abstract type for the given TypeMirror and AnnotatedTypeMirror
   */
  public static AbstractType createIgnoreInstantiated(
      AnnotatedTypeMirror type,
      TypeMirror typeMirror,
      @Nullable Theta map,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context,
      boolean ignoreAnnotations) {
    assert type != null;
    if (map == null) {
      return new ProperType(type, qualifierVars, context, ignoreAnnotations);
    }

    // See the comment about this test in `create`.
    Variable variable =
        typeMirror.getKind() == TypeKind.TYPEVAR ? map.get(type.getUnderlyingType()) : null;
    if (variable != null) {
      return new UseOfVariable(
          (AnnotatedTypeVariable) type, variable, qualifierVars, context, ignoreAnnotations);
    } else if (AnnotatedContainsInferenceVariable.hasAnyTypeVariable(
        map.getNotInstantiated(), type)) {
      return new InferenceType(type, map, qualifierVars, context, ignoreAnnotations);
    } else {
      return new ProperType(type, qualifierVars, context, ignoreAnnotations);
    }
  }

  /**
   * Creates abstract types for each TypeMirror. The created type is an {@link InferenceType} if it
   * contains any type variables that are mapped to inference variables as specified by {@code map}.
   * Or if the type is a type variable that is mapped to an inference variable, it will return that
   * {@link Variable}. Or if the type contains no type variables that are mapped in an inference
   * variable, a {@link ProperType} is returned.
   *
   * @param types the annotated type mirrors
   * @param map a mapping from type variable to inference variable, or null to treat no type
   *     variable as an inference variable
   * @param qualifierVars a mapping from polymorphic annotation to {@link QualifierVar}
   * @param context the context
   * @return the abstract type for the given TypeMirror and AnnotatedTypeMirror
   */
  public static List<AbstractType> create(
      List<AnnotatedTypeMirror> types,
      @Nullable Theta map,
      AnnotationMirrorMap<QualifierVar> qualifierVars,
      Java8InferenceContext context) {
    List<AbstractType> abstractTypes = new ArrayList<>();
    for (AnnotatedTypeMirror type : types) {
      abstractTypes.add(create(type, map, qualifierVars, context, false));
    }
    return abstractTypes;
  }

  @Override
  public AbstractType create(AnnotatedTypeMirror type, boolean ignoreAnnotations) {
    return create(type, map, qualifierVars, context, ignoreAnnotations);
  }

  @Override
  @SuppressWarnings("interning:not.interned") // maps should be ==
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InferenceType that = (InferenceType) o;
    if (!sameInferenceProblem(that)) {
      return false;
    }
    // Two types with different qualifierVars have different qualifiers, as getQualifiers() shows.
    if (!qualifierVars.equals(that.qualifierVars)) {
      return false;
    }

    return map == that.map && type.equals(that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inferenceProblemHashCode(), qualifierVars, type, Kind.INFERENCE_TYPE);
  }

  @Override
  public TypeMirror getJavaType() {
    return type.getUnderlyingType();
  }

  @Override
  public AnnotatedTypeMirror getAnnotatedType() {
    return type;
  }

  @Override
  public boolean isObject() {
    return false;
  }

  /**
   * Returns all inference variables mentioned in this type.
   *
   * @return all inference variables mentioned in this type
   */
  @Override
  public Collection<Variable> getInferenceVariables() {
    LinkedHashSet<Variable> variables = new LinkedHashSet<>();
    for (TypeVariable typeVar :
        ContainsInferenceVariable.getMentionedTypeVariables(
            map.getTypeVariables(), type.getUnderlyingType())) {
      @SuppressWarnings("nullness:assignment") // getMentionedTypeVariables returns keys of `map`
      Variable variable = map.get(typeVar);
      variables.add(variable);
    }
    return variables;
  }

  @Override
  public AbstractType applyInstantiations() {
    List<TypeVariable> typeVariables = new ArrayList<>();
    List<TypeMirror> arguments = new ArrayList<>();
    List<Variable> instantiations = new ArrayList<>();

    for (Variable alpha : map.values()) {
      if (alpha.getInstantiation() != null) {
        typeVariables.add(alpha.getJavaType());
        arguments.add(alpha.getBounds().getInstantiation().getJavaType());
        instantiations.add(alpha);
      }
    }
    if (typeVariables.isEmpty()) {
      return this;
    }

    TypeMirror newTypeJava =
        TypesUtils.substitute(getJavaType(), typeVariables, arguments, context.env);

    Map<TypeVariable, AnnotatedTypeMirror> mapping = new LinkedHashMap<>();

    for (Variable alpha : instantiations) {
      AnnotatedTypeMirror instantiation = alpha.getBounds().getInstantiation().getAnnotatedType();
      context.typeFactory.initializeAtm(instantiation);
      mapping.put(alpha.getJavaType(), instantiation);
    }

    AnnotatedTypeMirror newATM = typeFactory.getTypeVarSubstitutor().substitute(mapping, type);
    AbstractType newAbstractType =
        createIgnoreInstantiated(
            newATM, newTypeJava, map, AnnotationMirrorMap.emptyMap(), context, ignoreAnnotations);

    // Also apply instantiations to function type.
    AnnotatedExecutableType unsubedFunctionType = getFunctionType();
    if (unsubedFunctionType != null) {
      newAbstractType.functionType =
          (AnnotatedExecutableType)
              typeFactory.getTypeVarSubstitutor().substitute(mapping, unsubedFunctionType);
    }

    return newAbstractType;
  }

  @Override
  public String toString() {
    return "inference type: " + getJavaType();
  }

  @Override
  public Set<AbstractQualifier> getQualifiers() {
    return AbstractQualifier.create(
        getAnnotatedType().getPrimaryAnnotations(), qualifierVars, context);
  }

  /**
   * Is {@code this} a subtype of {@code superType}?
   *
   * @param superType the potential supertype; is a declared type with no parameters
   * @return if {@code this} is a subtype of {@code superType}, then return {@link
   *     ConstraintSet#TRUE}; otherwise, a false bound is returned
   */
  public ReductionResult isSubType(ProperType superType) {
    TypeMirror subType = getJavaType();
    TypeMirror superJavaType = superType.getJavaType();
    if (subType.getKind() == TypeKind.WILDCARD) {
      if (((WildcardType) subType).getExtendsBound() != null) {
        subType = ((WildcardType) subType).getExtendsBound();
      } else {
        subType = context.types.erasure((Type) subType);
      }
    }

    if (context.types.isSubtype((Type) subType, (Type) superJavaType)) {
      // If this is a wildcard, then `checkAnnotationSubtype` compares the annotations of the
      // extends bound, matching the narrowing of `subType` above:  the type hierarchy descends
      // into a wildcard subtype's extends bound.
      return checkAnnotationSubtype(superType);
    } else {
      return ConstraintSet.FALSE;
    }
  }
}
