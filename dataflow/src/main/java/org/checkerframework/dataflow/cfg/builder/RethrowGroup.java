package org.checkerframework.dataflow.cfg.builder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import org.plumelib.util.ArrayMap;

/**
 * Some of the types of the exceptions that reach a finally block (as defined in the documentation
 * of {@link TryFinallyFrame}), such as {@code java.io.IOException} and {@code
 * java.lang.RuntimeException}, all of which one copy of the finally block rethrows after the
 * finally block completes normally. Each type has an entry label: the label that an exception of
 * the type jumps to when it reaches the finally block (see {@link
 * TryFinallyFrame#getExceptionLabels}). The copy starts at the entry labels of all the types in its
 * group.
 */
/*package-private*/ final class RethrowGroup {

  /**
   * The entry label of each type in this group, in a deterministic order: the order in which the
   * types first reach the finally block. Like the map in {@link TryFinallyFrame}, it contains few
   * entries.
   */
  private final Map<TypeMirror, Label> entryLabels = new ArrayMap<>();

  /** Creates an empty RethrowGroup. */
  /*package-private*/ RethrowGroup() {}

  /**
   * Adds the given type to this group.
   *
   * @param type a type of exception that reaches the finally block and is not in this group
   * @param entryLabel the label that an exception of type {@code type} jumps to when it reaches the
   *     finally block
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
   * @param groups groups for the same finally block, no two of which contain the same type
   * @return a new group that contains every type in {@code groups}, in the order of {@code groups}
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
