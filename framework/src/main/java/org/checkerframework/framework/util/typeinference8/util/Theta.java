package org.checkerframework.framework.util.typeinference8.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
   * A key of {@link #map}: a wrapper around a type variable, whose {@code equals} method is {@link
   * TypesUtils#areSame(TypeVariable, TypeVariable)} rather than {@code TypeVariable.equals}.
   *
   * <p>This is not a record, because a record's generated {@code equals} would compare {@link
   * #typeVariable} using {@code equals}, which is exactly the comparison that this class exists to
   * avoid.
   */
  private static class Key {

    /** The type variable that this key stands for. */
    private final TypeVariable typeVariable;

    /**
     * Creates a key for {@code typeVariable}.
     *
     * @param typeVariable a type variable
     */
    Key(TypeVariable typeVariable) {
      this.typeVariable = typeVariable;
    }

    @Override
    public boolean equals(@Nullable Object other) {
      return other instanceof Key otherKey
          && TypesUtils.areSame(typeVariable, otherKey.typeVariable);
    }

    @Override
    public int hashCode() {
      return TypesUtils.hashCodeForAreSame(typeVariable);
    }

    @Override
    public String toString() {
      return typeVariable.toString();
    }
  }

  /** The mapping, from type variable to inference variable. */
  private final Map<Key, Variable> map = new LinkedHashMap<>();

  /**
   * The value that {@link #getTypeVariables} returns, or null if it has not been computed since the
   * most recent call to {@link #put}. A Theta is written once and then read many times, on hot
   * paths of inference, so it is worth caching.
   */
  private @Nullable Collection<? extends TypeVariable> typeVariables = null;

  /** Creates Theta. */
  public Theta() {}

  /**
   * Maps {@code typeVariable} to {@code variable}. If some type variable that is {@link
   * TypesUtils#areSame(TypeVariable, TypeVariable)} as {@code typeVariable} is already mapped, then
   * its inference variable is replaced, but {@link #getTypeVariables} continues to return the type
   * variable that was passed to the earlier call. That distinction does not matter to clients,
   * which compare type variables using {@code areSame}.
   *
   * <p>Callers that iterate over {@link #values} in lockstep with the type variables they passed to
   * this method, such as {@code CaptureBound}, depend on each call adding an entry rather than
   * replacing one. That holds because the type variables of a single declaration have distinct
   * simple names and the same enclosing element, so no two of them are {@code areSame}.
   *
   * @param typeVariable a type variable
   * @param variable the inference variable for {@code typeVariable}
   * @return the inference variable that {@code typeVariable} was previously mapped to, or null if
   *     it was not mapped
   */
  public @Nullable Variable put(TypeVariable typeVariable, Variable variable) {
    typeVariables = null;
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
   * Returns the inference variables, in the order in which they were added. The result is
   * unmodifiable.
   *
   * @return the inference variables, in the order in which they were added
   */
  public Collection<Variable> values() {
    return Collections.unmodifiableCollection(map.values());
  }

  /**
   * Returns the type variables that have an inference variable, in the order in which they were
   * added. The result is unmodifiable.
   *
   * <p>The result's {@code contains} method uses {@code equals}, which for a type variable is
   * reference equality. A client that wants this class's notion of key equality must compare the
   * elements using {@link TypesUtils#areSame(TypeVariable, TypeVariable)} itself, or call {@link
   * #get}.
   *
   * @return the type variables that have an inference variable
   */
  public Collection<? extends TypeVariable> getTypeVariables() {
    if (typeVariables == null) {
      List<TypeVariable> list = new ArrayList<>(map.size());
      for (Key key : map.keySet()) {
        list.add(key.typeVariable);
      }
      typeVariables = Collections.unmodifiableList(list);
    }
    return typeVariables;
  }

  /**
   * Returns the type variables that do not yet have a value, in the order in which they were added.
   * Unlike {@link #getTypeVariables}, the result is not cached, because a type variable acquires a
   * value without any call to {@link #put}.
   *
   * @return the type variables that do not yet have a value
   */
  public Collection<? extends TypeVariable> getNotInstantiated() {
    List<TypeVariable> list = new ArrayList<>();
    for (Map.Entry<Key, Variable> entry : map.entrySet()) {
      if (entry.getValue().getInstantiation() == null) {
        list.add(entry.getKey().typeVariable);
      }
    }
    return list;
  }

  @Override
  public String toString() {
    return map.toString();
  }
}
