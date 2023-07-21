package org.checkerframework.framework.util.typeinference8.util;

import java.util.LinkedHashMap;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.javacutil.TypesUtils;

/** A mapping from type variables to inference variables. */
public class Theta extends LinkedHashMap<TypeVariable, Variable> {
  private static final long serialVersionUID = 42L;

  private TypeVariable sames(TypeVariable other) {
    for (TypeVariable key : keySet()) {
      if (TypesUtils.areSame(key, other)) {
        return key;
      }
    }
    return other;
  }

  @Override
  public boolean containsKey(Object key) {
    if (key instanceof TypeVariable) {
      return super.containsKey(sames((TypeVariable) key));
    }
    return false;
  }

  @Override
  public Variable get(Object key) {
    if (key instanceof TypeVariable) {
      return super.get(sames((TypeVariable) key));
    }
    return super.get(key);
  }
}
