package org.checkerframework.framework.type;

import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Stores the result of {@link StructuralEqualityComparer} for type arguments.
 *
 * <p>This is similar to {@link SubtypeVisitHistory}, but both true and false results are stored.
 */
public class StructuralEqualityVisitHistory {

  /**
   * Types in this history are structurally equal. (Use {@link SubtypeVisitHistory} because it
   * implements a {@code Map<IPair<AnnotatedTypeMirror, AnnotatedTypeMirror>,
   * AnnotationMirrorSet>}).
   */
  private final SubtypeVisitHistory trueHistory;

  /**
   * Types in this history are not structurally equal. (Use {@link SubtypeVisitHistory} because it
   * implements a {@code Map<IPair<AnnotatedTypeMirror, AnnotatedTypeMirror>,
   * AnnotationMirrorSet>}).
   */
  private final SubtypeVisitHistory falseHistory;

  /** Creates an empty StructuralEqualityVisitHistory. */
  public StructuralEqualityVisitHistory() {
    this.trueHistory = new SubtypeVisitHistory();
    this.falseHistory = new SubtypeVisitHistory();
  }

  /**
   * Put result of comparing {@code type1} and {@code type2} for structural equality for the given
   * hierarchy.
   *
   * @param type1 the first type
   * @param type2 the second type
   * @param hierarchy the top of the relevant type hierarchy; only annotations from that hierarchy
   *     are considered
   * @param result true if {@code type1} is structurally equal to {@code type2}
   */
  public void put(
      AnnotatedTypeMirror type1,
      AnnotatedTypeMirror type2,
      AnnotationMirror hierarchy,
      boolean result) {
    if (result) {
      trueHistory.put(type1, type2, hierarchy, true);
      falseHistory.remove(type1, type2, hierarchy);
    } else {
      falseHistory.put(type1, type2, hierarchy, true);
      trueHistory.remove(type1, type2, hierarchy);
    }
  }

  /**
   * Returns true if the two types are structurally equal for the given hierarchy or {@code null} if
   * the types have not been visited for the given hierarchy.
   *
   * @param type1 the first type
   * @param type2 the second type
   * @param hierarchy the top of the relevant type hierarchy; only annotations from that hierarchy
   *     are considered
   * @return true if the two types are structurally equal for the given hierarchy or {@code null} if
   *     the types have not been visited for the given hierarchy
   */
  public @Nullable Boolean get(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotationMirror hierarchy) {
    if (falseHistory.contains(type1, type2, hierarchy)) {
      return false;
    } else if (trueHistory.contains(type1, type2, hierarchy)) {
      return true;
    }
    return null;
  }

  /**
   * Remove the result of comparing {@code type1} and {@code type2} for structural equality for the
   * given hierarchy.
   *
   * @param type1 the first type
   * @param type2 the second type
   * @param hierarchy the top of the relevant type hierarchy; only annotations from that hierarchy
   *     are considered
   */
  public void remove(
      AnnotatedTypeMirror type1, AnnotatedTypeMirror type2, AnnotationMirror hierarchy) {
    falseHistory.remove(type1, type2, hierarchy);
    trueHistory.remove(type1, type2, hierarchy);
  }
}
