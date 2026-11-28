package org.checkerframework.dataflow.cfg.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.plumelib.util.ArrayMap;

/**
 * A set of exception types that share one exceptional copy of a finally block.
 *
 * <p>An exceptional copy of a finally block runs when an exception reaches the finally block (as
 * defined in the documentation of {@link TryFinallyFrame}), and it rethrows the exception after the
 * finally block completes normally. The types in a group, such as {@code java.io.IOException} and
 * {@code java.lang.RuntimeException}, are some of the types of the exceptions that reach the
 * finally block.
 *
 * <p>For each of its types, a group stores the type's entry label: the label that the finally
 * block's {@link TryFinallyFrame} sends the type to, which is the target of the exceptional edges
 * for the type. The {@link TryFinallyFrame} creates the label (see {@link
 * TryFinallyFrame#getExceptionLabels}), and the group stores a reference to the same label. The
 * group's copy starts at the entry labels of all the types in the group.
 */
/*package-private*/ final class RethrowGroup {

  /**
   * For each type in this group, its entry label. Like the map in {@link TryFinallyFrame}, it
   * contains few entries.
   */
  private final Map<TypeMirror, Label> entryLabels = new ArrayMap<>();

  /** Creates an empty RethrowGroup. */
  /*package-private*/ RethrowGroup() {}

  /**
   * Adds the given type and label to this group.
   *
   * @param type a type of exception that reaches the finally block and is not in this group
   * @param entryLabel the label that the finally block's {@link TryFinallyFrame} sends {@code type}
   *     to: the target of the exceptional edges for {@code type} (see {@link
   *     TryFinallyFrame#getExceptionLabels})
   */
  /*package-private*/ void add(TypeMirror type, Label entryLabel) {
    entryLabels.put(type, entryLabel);
  }

  /**
   * Returns the types in this group.
   *
   * @return the types in this group, in a deterministic order
   */
  /*package-private*/ Set<TypeMirror> types() {
    return Collections.unmodifiableSet(entryLabels.keySet());
  }

  /**
   * Returns the entry labels of the types in this group.
   *
   * @return the entry labels of the types in this group, in the same order as {@link #types}
   */
  /*package-private*/ Collection<Label> entryLabels() {
    return Collections.unmodifiableCollection(entryLabels.values());
  }

  /**
   * Returns a new group that contains every type in the given groups.
   *
   * @param groups a list of groups for the same finally block; no type is in more than one of the
   *     groups
   * @return a new group that contains every type in {@code groups}, in a deterministic order
   */
  /*package-private*/ static RethrowGroup merge(List<RethrowGroup> groups) {
    RethrowGroup result = new RethrowGroup();
    for (RethrowGroup group : groups) {
      result.entryLabels.putAll(group.entryLabels);
    }
    return result;
  }

  @Override
  public String toString() {
    return "RethrowGroup" + entryLabels;
  }
}
