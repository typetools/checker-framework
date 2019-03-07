package org.checkerframework.framework.util.typeinference8.util;

import java.util.LinkedHashMap;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;

/** A mapping from type variable to inference variable. */
public class Theta extends LinkedHashMap<TypeVariable, Variable> {
    private static final long serialVersionUID = 42L;
}
