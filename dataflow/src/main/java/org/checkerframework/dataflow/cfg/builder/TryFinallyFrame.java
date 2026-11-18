package org.checkerframework.dataflow.cfg.builder;

import java.util.Collections;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.plumelib.util.ArraySet;
import org.plumelib.util.CollectionsP;

/**
 * A TryFinallyFrame applies to exceptions of any type. It records the type of each exception that
 * reaches it.
 */
/*package-private*/ class TryFinallyFrame implements TryFrame {
  /** The finally label. */
  protected final Label finallyLabel;

  /** The type utilities, used to avoid recording a type twice. */
  protected final Types types;

  /** The types of the exceptions that reach this frame, without duplicates. */
  private final Set<TypeMirror> reachingExceptions = new ArraySet<>();

  /**
   * Constructs a TryFinallyFrame.
   *
   * @param types the type utilities
   * @param finallyLabel finally label
   */
  public TryFinallyFrame(Types types, Label finallyLabel) {
    this.types = types;
    this.finallyLabel = finallyLabel;
  }

  @Override
  public String toString() {
    return "TryFinallyFrame: finallyLabel: " + finallyLabel;
  }

  @Override
  public boolean possibleLabels(TypeMirror thrown, Set<Label> labels) {
    if (!CollectionsP.anyMatch(reachingExceptions, t -> types.isSameType(t, thrown))) {
      reachingExceptions.add(thrown);
    }
    labels.add(finallyLabel);
    return true;
  }

  /**
   * Returns the types of the exceptions that reach this frame, without duplicates.
   *
   * @return the types of the exceptions, such as {@code java.io.IOException}, for which some node
   *     has an exceptional edge to {@link #finallyLabel}; empty if no node does
   */
  public Set<TypeMirror> getReachingExceptions() {
    return Collections.unmodifiableSet(reachingExceptions);
  }
}
