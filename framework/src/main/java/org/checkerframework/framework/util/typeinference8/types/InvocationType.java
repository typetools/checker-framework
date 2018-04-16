package org.checkerframework.framework.util.typeinference8.types;

import java.util.List;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;

public interface InvocationType {

    ExecutableType getJavaType();

    Iterable<? extends TypeMirror> getThrownTypes();

    AbstractType getReturnType(Theta map);

    /**
     * Returns a list of the parameter types of {@code InvocationType} where the vararg parameter
     * has been modified to match the arguments in {@code expression}.
     */
    List<AbstractType> getParameterTypes(Theta map, int size);

    List<? extends TypeVariable> getTypeVariables();

    boolean isVoid();

    List<AbstractType> getParameterTypes(Theta map);
}
