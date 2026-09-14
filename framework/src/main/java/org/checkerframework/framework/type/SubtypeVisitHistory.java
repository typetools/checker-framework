package org.checkerframework.framework.type;

import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.javacutil.AnnotationMirrorSet;
import org.plumelib.util.IPair;

/**
 * THIS CLASS IS DESIGNED FOR USE WITH DefaultTypeHierarchy, DefaultRawnessComparer, and
 * StructuralEqualityComparer ONLY.
 *
 * <p>VisitHistory tracks triples of (type1, type2, top), where type1 is a subtype of type2. It does
 * not track when type1 is not a subtype of type2; such entries are missing from the history.
 * Clients of this class can check whether or not they have visited an equivalent pair of
 * AnnotatedTypeMirrors already. This is necessary in order to halt visiting on recursive bounds.
 *
 * <p>This class is primarily used to implement isSubtype(ATM, ATM). The pair of types corresponds
 * to the subtype and the supertype being checked. A single subtype may be visited more than once,
 * but with a different supertype. For example, if the two types are {@code @A T extends @B
 * Serializable<T>} and {@code @C Serializable<?>}, then isSubtype is first called on those types
 * and then on {@code @B Serializable<T>} and {@code @C Serializable<?>}.
 *
 * <p>Callers should call {@link #clear} once an outermost (non-nested) {@code isSubtype} call
 * completes, so that the history does not accumulate entries across unrelated {@code isSubtype}
 * calls. Entries only need to persist for the duration of a single such call, to guard against
 * infinite recursion within it; letting the map grow for the whole compilation unit only makes
 * every subsequent lookup slower, for no benefit, since unrelated calls do not repeat the same
 * (type1, type2) pair.
 */
public class SubtypeVisitHistory {

  /**
   * The keys are pairs of types; the value is the set of qualifier hierarchy roots for which the
   * key is in a subtype relationship.
   */
  private final Map<IPair<AnnotatedTypeMirror, AnnotatedTypeMirror>, AnnotationMirrorSet> visited;

  /** Creates a new SubtypeVisitHistory. */
  public SubtypeVisitHistory() {
    this.visited = new HashMap<>();
  }

  /**
   * Put a visit for {@code type1}, {@code type2}, and {@code top} in the history. Has no effect if
   * isSubtype is false.
   *
   * @param type1 the first type
   * @param type2 the second type
   * @param currentTop the top of the relevant type hierarchy; only annotations from that hierarchy
   *     are considered
   * @param isSubtype true if {@code type1} is a subtype of {@code type2}; if false, this method
   *     does nothing
   */
  public void put(
      AnnotatedTypeMirror type1,
      AnnotatedTypeMirror type2,
      AnnotationMirror currentTop,
      boolean isSubtype) {
    if (!isSubtype) {
      // Only store information about subtype relations that hold.
      return;
    }
    IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> key = IPair.of(type1, type2);
    AnnotationMirrorSet hit = visited.get(key);

    if (hit != null) {
      hit.add(currentTop);
    } else {
      hit = new AnnotationMirrorSet();
      hit.add(currentTop);
      this.visited.put(key, hit);
    }
  }

  /** Remove {@code type1} and {@code type2}. */
  public void remove(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotationMirror currentTop) {
    IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> key = IPair.of(type1, type2);
    AnnotationMirrorSet hit = visited.get(key);
    if (hit != null) {
      hit.remove(currentTop);
      if (hit.isEmpty()) {
        visited.remove(key);
      }
    }
  }

  /**
   * Returns true if type1 and type2 (or an equivalent pair) have been passed to the put method
   * previously.
   *
   * @return true if an equivalent pair has already been added to the history
   */
  public boolean contains(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotationMirror currentTop) {
    IPair<AnnotatedTypeMirror, AnnotatedTypeMirror> key = IPair.of(type1, type2);
    AnnotationMirrorSet hit = visited.get(key);
    return hit != null && hit.contains(currentTop);
  }

  /** Removes all entries from the history. */
  public void clear() {
    visited.clear();
  }

  @Override
  public String toString() {
    return "VisitHistory( " + visited + " )";
  }
}
