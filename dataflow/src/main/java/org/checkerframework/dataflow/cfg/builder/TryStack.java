package org.checkerframework.dataflow.cfg.builder;

import java.util.ArrayDeque;
import java.util.Set;
import java.util.StringJoiner;
import javax.lang.model.type.TypeMirror;
import org.checkerframework.dataflow.util.MostlySingleton;

/**
 * An exception stack represents the set of all try-catch blocks in effect at a given point in a
 * program. It maps an exception type to a set of Labels and it maps a block exit (via return or
 * fall-through) to a single Label.
 */
class TryStack {
  /** The exit label. */
  protected final Label exitLabel;
  /** The try frames. */
  protected final ArrayDeque<TryFrame> frames;

  /**
   * Construct a TryStack.
   *
   * @param exitLabel exit label
   */
  public TryStack(Label exitLabel) {
    this.exitLabel = exitLabel;
    this.frames = new ArrayDeque<>();
  }

  /**
   * Push a new frame.
   *
   * @param frame the frame to push
   */
  public void pushFrame(TryFrame frame) {
    frames.addFirst(frame);
  }

  /** Pop a frame. */
  public void popFrame() {
    frames.removeFirst();
  }

  /**
   * Returns the set of possible {@link Label}s where control may transfer when an exception of the
   * given type is thrown.
   *
   * @param thrown an exception
   * @return where control may transfer when {@code thrown} is thrown
   */
  public Set<Label> possibleLabels(TypeMirror thrown) {
    // Work up from the innermost frame until the exception is known to be caught.
    Set<Label> labels = new MostlySingleton<>();
    for (TryFrame frame : frames) {
      if (frame.possibleLabels(thrown, labels)) {
        return labels;
      }
    }
    labels.add(exitLabel);
    return labels;
  }

  @Override
  public String toString() {
    StringJoiner sj = new StringJoiner(System.lineSeparator());
    sj.add("TryStack: exitLabel: " + this.exitLabel);
    if (this.frames.isEmpty()) {
      sj.add("No TryFrames.");
    }
    for (TryFrame tf : this.frames) {
      sj.add(tf.toString());
    }
    return sj.toString();
  }
}
