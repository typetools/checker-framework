package org.checkerframework.javacutil.typeinference8.types;

import java.util.LinkedHashMap;
import javax.lang.model.type.TypeVariable;

/** A mapping from type variable to inference variable. */
public class Theta extends LinkedHashMap<TypeVariable, Variable> {
    private static final long serialVersionUID = 42L;
}
