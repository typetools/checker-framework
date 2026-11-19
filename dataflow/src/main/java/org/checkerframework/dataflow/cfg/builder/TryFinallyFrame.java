package org.checkerframework.dataflow.cfg.builder;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.plumelib.util.ArrayMap;

/**
 * A TryFinallyFrame applies to exceptions of any type. It sends each type of exception to its own
 * label, which is the entry of a copy of the finally block that rethrows that type.
 */
/*package-private*/ class TryFinallyFrame implements TryFrame {
  /** The type utilities, used to give each type a single label. */
  protected final Types types;

  /**
   * A label for each type of exception that reaches this frame, in the order that the types first
   * reach it. No two keys are the same type.
   */
  private final Map<TypeMirror, Label> exceptionLabels = new ArrayMap<>();

  /**
   * Constructs a TryFinallyFrame.
   *
   * @param types the type utilities
   */
  public TryFinallyFrame(Types types) {
    this.types = types;
  }

  @Override
  public String toString() {
    return "TryFinallyFrame: exceptionLabels: " + exceptionLabels;
  }

  @Override
  public boolean possibleLabels(TypeMirror thrown, Set<Label> labels) {
    labels.add(labelFor(thrown));
    return true;
  }

  /**
   * Returns the label for the given type of exception, creating it if the type has none.
   *
   * @param thrown a type of exception that reaches this frame
   * @return the label for {@code thrown}
   */
  private Label labelFor(TypeMirror thrown) {
    for (Map.Entry<TypeMirror, Label> entry : exceptionLabels.entrySet()) {
      if (types.isSameType(entry.getKey(), thrown)) {
        return entry.getValue();
      }
    }
    Label result = new Label();
    exceptionLabels.put(thrown, result);
    return result;
  }

  /**
   * Returns a label for each type of exception that reaches this frame.
   *
   * @return a map from each type of exception that reaches this frame, such as {@code
   *     java.io.IOException}, to its label, in the order that the types first reach this frame;
   *     empty if no exception reaches this frame
   */
  public Map<TypeMirror, Label> getExceptionLabels() {
    return Collections.unmodifiableMap(exceptionLabels);
  }
}
