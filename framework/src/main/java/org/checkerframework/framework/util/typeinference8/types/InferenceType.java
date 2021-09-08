package org.checkerframework.framework.util.typeinference8.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;
import org.checkerframework.framework.util.typeinference8.util.Theta;
import org.checkerframework.javacutil.TypeAnnotationUtils;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A type-like structure that contains at least one inference variable, but is not an inference
 * variable.
 */
public class InferenceType extends AbstractType {

  /**
   * The underlying Java type. It contains type variables that are mapped to inference variables in
   * {@code map}.
   */
  private final TypeMirror typeMirror;

  /**
   * The underlying Java type. It contains type variables that are mapped to inference variables in
   * {@code map}.
   */
  private final AnnotatedTypeMirror type;

  /** A mapping of type variables to inference variables. */
  private final Theta map;

  private InferenceType(
      AnnotatedTypeMirror type, TypeMirror typeMirror, Theta map, Java8InferenceContext context) {
    super(context);
    assert type.getKind() == typeMirror.getKind();
    this.type = type;
    this.typeMirror = typeMirror;
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
   * it will return that {@link Variable}. Or if {@code type} contains no type variables that are
   * mapped in an inference variable, a {@link ProperType} is returned.
   */
  public static AbstractType create(
      AnnotatedTypeMirror type, TypeMirror typeMirror, Theta map, Java8InferenceContext context) {
    assert type != null;
    typeMirror = TypeAnnotationUtils.unannotatedType(typeMirror);
    if (map == null) {
      return new ProperType(type, typeMirror, context);
    }
    if (typeMirror.getKind() == TypeKind.TYPEVAR && map.containsKey(typeMirror)) {
      return map.get(typeMirror);
    } else if (ContainsInferenceVariable.hasAnyTypeVariable(map.keySet(), typeMirror)) {
      return new InferenceType(type, typeMirror, map, context);
    } else {
      return new ProperType(type, typeMirror, context);
    }
  }

  /**
   * Creates abstract types for each TypeMirror. The created type is an {@link InferenceType} if it
   * contains any type variables that are mapped to inference variables as specified by {@code map}.
   * Or if the type is a type variable that is mapped to an inference variable, it will return that
   * {@link Variable}. Or if the type contains no type variables that are mapped in an inference
   * variable, a {@link ProperType} is returned.
   */
  public static List<AbstractType> create(
      List<AnnotatedTypeMirror> types,
      List<? extends TypeMirror> typeMirrors,
      Theta map,
      Java8InferenceContext context) {
    List<AbstractType> abstractTypes = new ArrayList<>();
    Iterator<? extends TypeMirror> iter = typeMirrors.iterator();
    for (AnnotatedTypeMirror type : types) {
      abstractTypes.add(create(type, iter.next(), map, context));
    }
    return abstractTypes;
  }

  @Override
  public AbstractType create(AnnotatedTypeMirror type, TypeMirror typeMirror) {
    return create(type, typeMirror, map, context);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    InferenceType variable = (InferenceType) o;
    if (!type.equals(variable.type)) {
      return false;
    }
    return context.modelTypes.isSameType(typeMirror, variable.typeMirror);
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + Kind.INFERENCE_TYPE.hashCode();
    return result;
  }

  @Override
  public TypeMirror getJavaType() {
    return typeMirror;
  }

  @Override
  public AnnotatedTypeMirror getAnnotatedType() {
    return type;
  }

  @Override
  public boolean isObject() {
    return false;
  }

  /** @return all inference variables mentioned in this type. */
  @Override
  public Collection<Variable> getInferenceVariables() {
    LinkedHashSet<Variable> variables = new LinkedHashSet<>();
    for (TypeVariable typeVar :
        ContainsInferenceVariable.getMentionedTypeVariables(map.keySet(), typeMirror)) {
      variables.add(map.get(typeVar));
    }
    return variables;
  }

  @Override
  public AbstractType applyInstantiations(List<Variable> instantiations) {
    List<TypeVariable> typeVariables = new ArrayList<>(instantiations.size());
    List<TypeMirror> arguments = new ArrayList<>(instantiations.size());

    for (Variable alpha : instantiations) {
      if (map.containsValue(alpha)) {
        typeVariables.add(alpha.getJavaType());
        arguments.add(alpha.getBounds().getInstantiation().getJavaType());
      }
    }
    if (typeVariables.isEmpty()) {
      return this;
    }

    TypeMirror newTypeJava =
        TypesUtils.substitute(typeMirror, typeVariables, arguments, context.env);

    Map<TypeVariable, AnnotatedTypeMirror> mapping = new LinkedHashMap<>();

    for (Variable alpha : instantiations) {
      if (map.containsValue(alpha)) {
        AnnotatedTypeMirror instantiation = alpha.getBounds().getInstantiation().getAnnotatedType();
        mapping.put(alpha.getJavaType(), instantiation);
      }
    }
    if (map.isEmpty()) {
      return this;
    }

    AnnotatedTypeMirror newType = typeFactory.getTypeVarSubstitutor().substitute(mapping, type);
    return create(newType, newTypeJava, map, context);
  }

  @Override
  public String toString() {
    return "inference type: " + typeMirror;
  }
}
