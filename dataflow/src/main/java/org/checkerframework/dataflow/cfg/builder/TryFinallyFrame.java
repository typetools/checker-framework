package org.checkerframework.dataflow.cfg.builder;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import org.plumelib.util.ArrayMap;

/**
 * A TryFinallyFrame applies to exceptions of any type. It sends each type of exception that reaches
 * it to its own label. The label is the entry of a copy of the finally block that rethrows that
 * type, and possibly other types, after the finally block completes normally.
 *
 * <p>A type reaches a TryFinallyFrame if, while the frame is on the try stack, the CFG builder
 * creates a node that it records as possibly throwing that exact type, and no frame nearer the top
 * of the try stack is certain to catch the type. For example, the CFG builder records a method
 * invocation as possibly throwing each type in the method's {@code throws} clause, plus {@code
 * java.lang.RuntimeException} and {@code java.lang.Error}. A subtype of a type that reaches the
 * frame does not reach the frame unless some node is recorded as throwing the subtype itself. For
 * example, if the only node in a try block is an invocation of a static method that has no {@code
 * throws} clause, then {@code java.lang.RuntimeException} and {@code java.lang.Error} reach the
 * frame, but {@code java.lang.NullPointerException} does not.
 */
/*package-private*/ class TryFinallyFrame implements TryFrame {
  /** The type utilities, used to give each type a single label. */
  protected final Types types;

  /**
   * A label for each type of exception that reaches this frame (as defined in the documentation of
   * {@link TryFinallyFrame}). No two keys are the same type according to {@link Types#isSameType},
   * even if {@link TypeMirror#equals} would distinguish them.
   *
   * <p>The keys are in a deterministic order: the order in which the CFG builder first creates a
   * node that throws the type to this frame. Any deterministic order would do; it makes the CFG,
   * such as the order of the copies of the finally block, the same on every run.
   *
   * <p>The map contains few entries, usually only a handful, so it is an {@link ArrayMap}. The
   * types that reach a frame are {@code java.lang.RuntimeException} and {@code java.lang.Error},
   * which every invocation is recorded as possibly throwing; a few other unchecked types such as
   * {@code java.lang.NullPointerException} and {@code java.lang.ArithmeticException}; the checked
   * exceptions in the {@code throws} clauses of the invoked methods and constructors; and the
   * static types of the expressions in {@code throw} statements, such as {@code
   * java.io.IOException} for {@code throw new IOException()}. The copies of nested finally blocks
   * rethrow types of these same kinds.
   */
  private final Map<TypeMirror, Label> exceptionLabels = new ArrayMap<>();

  /**
   * Constructs a TryFinallyFrame.
   *
   * @param types a {@link Types}, used to give each type of exception a single label
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
   * Returns a label for each type of exception that reaches this frame (as defined in the
   * documentation of {@link TryFinallyFrame}).
   *
   * @return a map from each type of exception that reaches this frame, such as {@code
   *     java.io.IOException}, to its label, in a deterministic order (see {@link
   *     #exceptionLabels}); empty if no exception reaches this frame
   */
  public Map<TypeMirror, Label> getExceptionLabels() {
    return Collections.unmodifiableMap(exceptionLabels);
  }
}
