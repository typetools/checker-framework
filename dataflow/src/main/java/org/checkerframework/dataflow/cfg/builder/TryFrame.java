package org.checkerframework.dataflow.cfg.builder;

import java.util.Set;
import javax.lang.model.type.TypeMirror;

/** A TryFrame maps a thrown exception type to a set of possible control-flow successors. */
interface TryFrame {
  /**
   * Given a type of thrown exception, add the set of possible control flow successor {@link Label}s
   * to the argument set. Return true if the exception is known to be caught by one of those labels
   * and false if it may propagate still further.
   */
  public boolean possibleLabels(TypeMirror thrown, Set<Label> labels);
}
