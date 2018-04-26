package org.checkerframework.framework.util.typeinference8;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.type.AnnotatedTypeMirror;
import org.checkerframework.framework.util.typeinference8.typemirror.type.InferenceTypeMirror;
import org.checkerframework.framework.util.typeinference8.typemirror.type.ProperTypeMirror;
import org.checkerframework.framework.util.typeinference8.typemirror.type.VariableTypeMirror;
import org.checkerframework.framework.util.typeinference8.types.AbstractType;
import org.checkerframework.framework.util.typeinference8.types.ContainsInferenceVariable;
import org.checkerframework.framework.util.typeinference8.types.InferenceType;
import org.checkerframework.framework.util.typeinference8.types.Theta;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.framework.util.typeinference8.util.Java8InferenceContext;

public class InferenceAnnotatedType extends AbstractAnnotatedType implements InferenceType {
    /**
     * The underlying Java type. It contains type variables that are mapped to inference variables
     * in {@code map}.
     */
    private final AnnotatedTypeMirror type;

    /** A mapping of type variables to inference variables. */
    private final Theta map;

    private InferenceAnnotatedType(
            AnnotatedTypeMirror type, Theta map, Java8InferenceContext context) {
        super(context);
        this.type = type;
        this.map = map;
    }
    /**
     * Creates an abstract type for the given TypeMirror. The created type is an {@link
     * InferenceTypeMirror} if {@code type} contains any type variables that are mapped to inference
     * variables as specified by {@code map}. Or if {@code type} is a type variable that is mapped
     * to an inference variable, it will return that {@link VariableTypeMirror}. Or if {@code type}
     * contains no type variables that are mapped in an inference variable, a {@link
     * ProperTypeMirror} is returned.
     */
    static AbstractType create(AnnotatedTypeMirror type, Theta map, Java8InferenceContext context) {
        assert type != null;
        if (map == null) {
            return new ProperAnnotatedType(type, context);
        }
        if (type.getKind() == TypeKind.TYPEVAR && map.containsKey(type.getUnderlyingType())) {
            return map.get(type.getUnderlyingType());
        } else if (ContainsInferenceVariable.hasAnyTypeVariable(
                map.keySet(), type.getUnderlyingType())) {
            return new InferenceAnnotatedType(type, map, context);
        } else {
            return new ProperAnnotatedType(type, context);
        }
    }

    /**
     * Creates abstract types for each TypeMirror. The created type is an {@link
     * InferenceTypeMirror} if {@code type} contains any type variables that are mapped to inference
     * variables as specified by {@code map}. Or if {@code type} is a type variable that is mapped
     * to an inference variable, it will return that {@link VariableTypeMirror}. Or if {@code type}
     * contains no type variables that are mapped in an inference variable, a {@link
     * ProperTypeMirror} is returned.
     */
    static List<AbstractType> create(
            List<? extends AnnotatedTypeMirror> types, Theta map, Java8InferenceContext context) {
        List<AbstractType> abstractTypes = new ArrayList<>();
        for (AnnotatedTypeMirror type : types) {
            abstractTypes.add(create(type, map, context));
        }
        return abstractTypes;
    }

    @Override
    public AbstractType create(AnnotatedTypeMirror type) {
        return create(type, map, context);
    }

    @Override
    public AnnotatedTypeMirror getAnnotatedType() {
        return type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        InferenceAnnotatedType variable = (InferenceAnnotatedType) o;
        return context.modelTypes.isSameType(
                type.getUnderlyingType(), variable.type.getUnderlyingType());
    }

    @Override
    public int hashCode() {
        int result = type.toString().hashCode();
        result = 31 * result + Kind.INFERENCE_TYPE.hashCode();
        return result;
    }

    @Override
    public TypeMirror getJavaType() {
        return type.getUnderlyingType();
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
                ContainsInferenceVariable.getMentionedTypeVariables(
                        map.keySet(), type.getUnderlyingType())) {
            variables.add(map.get(typeVar));
        }
        return variables;
    }

    @Override
    public AbstractType applyInstantiations(List<Variable> instantiations) {

        Map<TypeVariable, AnnotatedTypeMirror> mapping = new LinkedHashMap<>();

        for (Variable alpha : instantiations) {
            if (map.containsValue(alpha)) {
                AnnotatedTypeMirror instantiation =
                        ((AbstractAnnotatedType) alpha.getBounds().getInstantiation())
                                .getAnnotatedType();
                mapping.put(alpha.getJavaType(), instantiation);
            }
        }
        if (map.isEmpty()) {
            return this;
        }

        AnnotatedTypeMirror newType = typeFactory.getTypeVarSubstitutor().substitute(mapping, type);
        return create(newType, map, context);
    }

    @Override
    public String toString() {
        return "inference type: " + type;
    }
}
