package org.checkerframework.dataflow.cfg.builder;

import java.util.Set;
import javax.lang.model.type.TypeMirror;

/** A TryFrame maps a thrown exception type to a set of possible control-flow successors. */
/*package-private*/ interface TryFrame {
  /**
   * Given a type of thrown exception, adds the set of possible control flow successor {@link
   * Label}s to the argument set. Returns true if the exception is known to be caught by one of
   * those labels and false if it might propagate still further.
   */
  public boolean possibleLabels(TypeMirror thrown, Set<Label> labels);
}
