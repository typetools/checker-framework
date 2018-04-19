package org.checkerframework.javacutil.typeinference8.types;

import java.util.List;
import javax.lang.model.type.ExecutableType;

public interface InvocationType {

    ExecutableType getJavaType();

    List<? extends AbstractType> getThrownTypes(Theta map);

    AbstractType getReturnType(Theta map);

    /**
     * Returns a list of the parameter types of {@code InvocationType} where the vararg parameter
     * has been modified to match the arguments in {@code expression}.
     */
    List<AbstractType> getParameterTypes(Theta map, int size);

    boolean hasTypeVariables();

    boolean isVoid();

    List<AbstractType> getParameterTypes(Theta map);
}
