package org.checkerframework.checker.resourceleak;

import com.google.common.collect.ImmutableSet;
import com.sun.tools.javac.code.Type;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.checkerframework.checker.signature.qual.CanonicalName;
import org.checkerframework.dataflow.qual.Pure;

/**
 * A set of types.
 *
 * <p>Important properties of this class:
 *
 * <ul>
 *   <li>No defined equality: in general, equality between these sets is prohibitively difficult to
 *       compute, and therefore this class uses <i>reference equality</i>.
 *   <li>Unknown size: it is not possible to know the true size of a set like {@link
 *       #allSubtypes(TypeMirror)}. By extension, it is not possible to iterate over a {@code
 *       SetOfTypes}.
 *   <li>Immutable: instances of this class can be created but not modified.
 * </ul>
 */
public interface SetOfTypes {

  /**
   * Returns true if this set contains the given type.
   *
   * @param typeUtils a {@code Types} object for computing the relationships between types
   * @param type the type in question
   * @return true if this set contains {@code type}, or false otherwise
   */
  @Pure
  boolean contains(Types typeUtils, TypeMirror type);

  /** An empty set of types. */
  SetOfTypes EMPTY = (typeUtils, type) -> false;

  /**
   * Create a set containing exactly the given type, but not its subtypes.
   *
   * @param t the type
   * @return a set containing only {@code t}
   */
  @Pure
  static SetOfTypes singleton(TypeMirror t) {
    return (typeUtils, u) -> typeUtils.isSameType(t, u);
  }

  /**
   * Create a set containing the given type and all of its subtypes.
   *
   * @param t the type
   * @return a set containing {@code t} and its subtypes
   */
  @Pure
  static SetOfTypes allSubtypes(TypeMirror t) {
    return (typeUtils, u) -> typeUtils.isSubtype(u, t);
  }

  /**
   * Create a set containing exactly the types with the given names, but not their subtypes.
   *
   * @param names the type names
   * @return a set containing only the named types
   */
  @Pure
  static SetOfTypes anyOfTheseNames(ImmutableSet<@CanonicalName String> names) {
    return (typeUtils, u) ->
        u instanceof Type && names.contains(((Type) u).tsym.getQualifiedName().toString());
  }

  /**
   * Create a set representing the union of all the given sets.
   *
   * @param typeSets an array of sets
   * @return the union of the given sets
   */
  @Pure
  static SetOfTypes union(SetOfTypes... typeSets) {
    switch (typeSets.length) {
      case 0:
        return EMPTY;
      case 1:
        return typeSets[0];
      default:
        return (typeUtils, type) -> {
          for (SetOfTypes set : typeSets) {
            if (set.contains(typeUtils, type)) {
              return true;
            }
          }
          return false;
        };
    }
  }
}
