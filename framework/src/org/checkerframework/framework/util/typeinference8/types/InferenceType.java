package org.checkerframework.framework.util.typeinference8.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.util.Context;
import org.checkerframework.javacutil.TypesUtils;

/** A type like structure that contains inference variables. */
public class InferenceType extends AbstractType {

    /**
     * The underlying Java type. It contains type variables that are mapped to inference variables
     * in {@code map}.
     */
    private final TypeMirror type;

    /** A mapping of type variables to inference variables. */
    private final Theta map;

    private InferenceType(TypeMirror type, Theta map, Context context) {
        super(context);
        this.type = type;
        this.map = map;
    }

    /**
     * Creates an abstract type for the given TypeMirror. The created type is an {@link
     * InferenceType} if {@code type} contains any type variables that are mapped to inference
     * variables as specified by {@code map}. Or if {@code type} is a type variable that is mapped
     * to an inference variable, it will return that {@link Variable}. Or if {@code type} contains
     * no type variables that are mapped in an inference variable, a {@link ProperType} is returned.
     */
    public static AbstractType create(TypeMirror type, Theta map, Context context) {
        assert type != null;
        if (type.getKind() == TypeKind.TYPEVAR && map.containsKey(type)) {
            return map.get(type);
        } else if (ContainsInferenceVariable.hasAnyTypeVariable(map.keySet(), type)) {
            return new InferenceType(type, map, context);
        } else {
            return new ProperType(type, context);
        }
    }

    /**
     * Creates abstract types for each TypeMirror. The created type is an {@link InferenceType} if
     * {@code type} contains any type variables that are mapped to inference variables as specified
     * by {@code map}. Or if {@code type} is a type variable that is mapped to an inference
     * variable, it will return that {@link Variable}. Or if {@code type} contains no type variables
     * that are mapped in an inference variable, a {@link ProperType} is returned.
     */
    public static List<AbstractType> create(
            List<? extends TypeMirror> types, Theta map, Context context) {
        List<AbstractType> abstractTypes = new ArrayList<>();
        for (TypeMirror type : types) {
            abstractTypes.add(create(type, map, context));
        }
        return abstractTypes;
    }

    @Override
    public AbstractType create(TypeMirror type) {
        return create(type, map, context);
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
        return context.factory.getContext().getTypeUtils().isSameType(type, variable.type);
    }

    @Override
    public int hashCode() {
        int result = type.toString().hashCode();
        result = 31 * result + Kind.INFERENCE_TYPE.hashCode();
        return result;
    }

    @Override
    public TypeMirror getJavaType() {
        return type;
    }

    @Override
    public boolean isObject() {
        return false;
    }

    @Override
    public Kind getKind() {
        return Kind.INFERENCE_TYPE;
    }

    /** @return all inference variables mentioned in this type. */
    @Override
    public Collection<Variable> getInferenceVariables() {
        LinkedHashSet<Variable> variables = new LinkedHashSet<>();
        for (TypeVariable typeVar :
                ContainsInferenceVariable.getMentionedTypeVariables(map.keySet(), type)) {
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
                arguments.add(alpha.getInstantiation().getJavaType());
            }
        }
        if (typeVariables.isEmpty()) {
            return this;
        }

        TypeMirror newType = TypesUtils.substitute(type, typeVariables, arguments, context.env);
        return create(newType, map, context);
    }

    @Override
    public String toString() {
        return "inference type: " + type;
    }
}
