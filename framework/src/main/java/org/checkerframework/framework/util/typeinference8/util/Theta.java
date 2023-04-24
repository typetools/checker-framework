package org.checkerframework.framework.util.typeinference8.util;

import java.util.LinkedHashMap;
import javax.lang.model.element.Element;
import javax.lang.model.element.Name;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.framework.util.typeinference8.types.Variable;

/** A mapping from type variables to inference variables. */
public class Theta extends LinkedHashMap<TypeVariable, Variable> {
  private static final long serialVersionUID = 42L;

  private TypeVariable sames(TypeVariable other) {

    Name otherName = other.asElement().getSimpleName();
    Element otherEnclosingElement = other.asElement().getEnclosingElement();

    for (TypeVariable key : keySet()) {
      if (key == other) {
        return other;
      }
      if (key.asElement().getSimpleName().contentEquals(otherName)
          && otherEnclosingElement.equals(key.asElement().getEnclosingElement())) {
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
