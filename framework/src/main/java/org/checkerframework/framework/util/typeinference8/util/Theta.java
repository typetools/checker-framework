package org.checkerframework.framework.util.typeinference8.util;

import java.util.LinkedHashMap;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.javacutil.TypesUtils;

/** A mapping from type variables to inference variables. */
public class Theta extends LinkedHashMap<TypeVariable, Variable> {

  /** serialVersionUID */
  private static final long serialVersionUID = 42L;

  /**
   * Returns the type variable in the key set that is {@link TypesUtils#areSame(TypeVariable,
   * TypeVariable)} as {@code typeVariable}.
   *
   * @param typeVariable a type variable
   * @return the type variable in the key set that is {@link TypesUtils#areSame(TypeVariable,
   *     TypeVariable)} as {@code typeVariable}
   */
  private TypeVariable getTypeVariable(TypeVariable typeVariable) {
    for (TypeVariable key : keySet()) {
      if (TypesUtils.areSame(key, typeVariable)) {
        return key;
      }
    }
    return typeVariable;
  }

  @Override
  public boolean containsKey(Object key) {
    if (key instanceof TypeVariable) {
      return super.containsKey(getTypeVariable((TypeVariable) key));
    }
    return false;
  }

  @Override
  public Variable get(Object key) {
    if (key instanceof TypeVariable) {
      return super.get(getTypeVariable((TypeVariable) key));
    }
    return super.get(key);
  }
}
