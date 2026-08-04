package org.checkerframework.framework.util.typeinference8.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.lang.model.element.Element;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.util.typeinference8.types.Variable;
import org.checkerframework.javacutil.TypesUtils;

/**
 * A mapping from type variables to inference variables.
 *
 * <p>Two type variables are the same key if {@link TypesUtils#areSame(TypeVariable, TypeVariable)}
 * returns true for them; they need not be the same object. (Two objects for the same type variable
 * arise when a type has undergone type variable substitution, or when the type variable is the type
 * of a tree created by {@link org.checkerframework.javacutil.trees.TreeBuilder}.) That is why this
 * class is not a {@code Map}: a {@code Map} would compare keys using {@code TypeVariable.equals},
 * which javac's {@code Type.TypeVar} does not override and which is therefore reference equality.
 *
 * <p>Iteration order is insertion order, as for {@link LinkedHashMap}.
 */
public class Theta {

  /**
   * A key of {@link #map}: a type variable whose {@code equals} method is {@link
   * TypesUtils#areSame(TypeVariable, TypeVariable)}, that is, whose declaration has the same simple
   * name and the same enclosing element.
   */
  private static class Key {

    /** The type variable that this key stands for. */
    private final TypeVariable typeVariable;

    /** The simple name of the type variable's declaration. */
    private final String name;

    /** The element that encloses the type variable's declaration. */
    private final @Nullable Element enclosingElement;

    /**
     * Creates a key for {@code typeVariable}.
     *
     * @param typeVariable a type variable
     */
    Key(TypeVariable typeVariable) {
      Element element = typeVariable.asElement();
      this.typeVariable = typeVariable;
      this.name = element.getSimpleName().toString();
      this.enclosingElement = element.getEnclosingElement();
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return other instanceof Key otherKey
          && name.equals(otherKey.name)
          && Objects.equals(enclosingElement, otherKey.enclosingElement);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, enclosingElement);
    }

    @Override
    public String toString() {
      return typeVariable.toString();
    }
  }

  /** The mapping, from type variable to inference variable. */
  private final Map<Key, Variable> map = new LinkedHashMap<>();

  /** Creates Theta. */
  public Theta() {}

  /**
   * Maps {@code typeVariable} to {@code variable}. Any existing mapping for a type variable that is
   * {@link TypesUtils#areSame(TypeVariable, TypeVariable)} as {@code typeVariable} is replaced.
   *
   * @param typeVariable a type variable
   * @param variable the inference variable for {@code typeVariable}
   * @return the inference variable that {@code typeVariable} was previously mapped to, or null if
   *     it was not mapped
   */
  public @Nullable Variable put(TypeVariable typeVariable, Variable variable) {
    return map.put(new Key(typeVariable), variable);
  }

  /**
   * Returns the inference variable for {@code type}, or null if there is none. {@code type} need
   * not be the same object as the type variable that was passed to {@link #put}; it is enough that
   * the two are {@link TypesUtils#areSame(TypeVariable, TypeVariable)}.
   *
   * @param type a type; if it is not a type variable, then this method returns null
   * @return the inference variable for {@code type}, or null if there is none
   */
  public @Nullable Variable get(TypeMirror type) {
    if (type instanceof TypeVariable typeVariable) {
      return map.get(new Key(typeVariable));
    }
    return null;
  }

  /**
   * Returns true if {@code variable} is the inference variable for some type variable.
   *
   * @param variable an inference variable
   * @return true if {@code variable} is the inference variable for some type variable
   */
  public boolean containsValue(Variable variable) {
    return map.containsValue(variable);
  }

  /**
   * Returns the inference variables, in the order in which they were added.
   *
   * @return the inference variables, in the order in which they were added
   */
  public Collection<Variable> values() {
    return map.values();
  }

  /**
   * Returns the type variables that have an inference variable, in the order in which they were
   * added.
   *
   * @return the type variables that have an inference variable
   */
  public Collection<? extends TypeVariable> getTypeVariables() {
    List<TypeVariable> list = new ArrayList<>(map.size());
    for (Key key : map.keySet()) {
      list.add(key.typeVariable);
    }
    return list;
  }

  /**
   * Returns a list of type variables that do not yet have a value.
   *
   * @return a list of type variables that do not yet have a value
   */
  public Collection<? extends TypeVariable> getNotInstantiated() {
    List<TypeVariable> list = new ArrayList<>();
    map.forEach(
        (key, var) -> {
          if (var.getInstantiation() == null) {
            list.add(key.typeVariable);
          }
        });
    return list;
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
